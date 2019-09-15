package com.keepgulp.megaapi.client;

import static com.keepgulp.megaapi.client.MainPanel.*;
import static com.keepgulp.megaapi.client.MiscTools.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public class ChunkDownloader implements Runnable, SecureSingleThreadNotifiable {

    public static final double SLOW_PROXY_PERC = 0.3;
    private final boolean FORCE_SMART_PROXY = false; //True for debugging SmartProxy
    private final int _id;
    private final Download _download;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private volatile boolean _error_wait;
    private boolean _notified;

    private String _current_smart_proxy;

    public ChunkDownloader(int id, Download download) {
        _notified = false;
        _exit = false;
        _secure_notify_lock = new Object();
        _id = id;
        _download = download;
        _current_smart_proxy = null;
        _error_wait = false;

    }

    public String getCurrent_smart_proxy() {
        return _current_smart_proxy;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    public boolean isExit() {
        return _exit;
    }

    public Download getDownload() {
        return _download;
    }

    public int getId() {
        return _id;
    }

    public boolean isError_wait() {
        return _error_wait;
    }

    public void setError_wait(boolean error_wait) {
        _error_wait = error_wait;
    }

    @Override
    public void secureNotify() {
        synchronized (_secure_notify_lock) {

            _notified = true;

            _secure_notify_lock.notify();
        }
    }

    @Override
    public void secureWait() {

        synchronized (_secure_notify_lock) {
            while (!_notified) {

                try {
                    _secure_notify_lock.wait();
                } catch (InterruptedException ex) {
                    _exit = true;
                    LOG.log(Level.SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }

    @Override
    public void run() {

        Thread.currentThread().setPriority(Math.max(Thread.currentThread().getPriority() - 1, Thread.MIN_PRIORITY));

        LOG.log(Level.INFO, "{0} Worker [{1}]: let''s do some work!", new Object[]{Thread.currentThread().getName(), _id});

        HttpURLConnection con;

        try {

            int http_error = 0, http_status = -1;

            boolean timeout = false, chunk_error = false, slow_proxy = false, turbo_mode = false;

            String worker_url = null;

            long init_chunk_time = -1, finish_chunk_time = -1, pause_init_time, paused = 0L;

            SmartMegaProxyManager proxy_manager = getProxy_manager();

            if (FORCE_SMART_PROXY) {

                _current_smart_proxy = proxy_manager.getFastestProxy();

                getDownload().enableTurboMode();

                turbo_mode = true;
            }

            while (!_download.getMain_panel().isExit() && !_exit && !_download.isStopped()) {

                if (_download.isPaused() && !_download.isStopped()) {

                    _download.pause_worker();

                    secureWait();

                }

                if (worker_url == null || http_error == 403) {

                    worker_url = _download.getDownloadUrlForWorker();
                }

                long chunk_id = _download.nextChunkId();

                long chunk_offset = ChunkManager.calculateChunkOffset(chunk_id, Download.CHUNK_SIZE_MULTI);

                long chunk_size = ChunkManager.calculateChunkSize(chunk_id, _download.getFile_size(), chunk_offset, Download.CHUNK_SIZE_MULTI);

                ChunkManager.checkChunkID(chunk_id, _download.getFile_size(), chunk_offset);

                String chunk_url = ChunkManager.genChunkUrl(worker_url, _download.getFile_size(), chunk_offset, chunk_size);

                if ((_current_smart_proxy != null || http_error == 509) && isUse_smart_proxy() && !isUse_proxy()) {

                    if (_current_smart_proxy != null && (slow_proxy || chunk_error)) {

                        proxy_manager.blockProxy(_current_smart_proxy);

                        _current_smart_proxy = proxy_manager.getFastestProxy();

                        Logger.getLogger(MiscTools.class.getName()).log(Level.WARNING, "{0}: worker {1} excluding proxy -> {2}", new Object[]{Thread.currentThread().getName(), _id, _current_smart_proxy});

                    } else if (_current_smart_proxy == null) {

                        _current_smart_proxy = proxy_manager.getFastestProxy();

                        if (!turbo_mode) {
                            getDownload().enableTurboMode();

                            turbo_mode = true;
                        }

                    }

                    if (_current_smart_proxy != null) {

                        String[] proxy_info = _current_smart_proxy.split(":");

                        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], Integer.parseInt(proxy_info[1])));

                        URL url = new URL(chunk_url);

                        con = (HttpURLConnection) url.openConnection(proxy);

                    } else {

                        LOG.log(Level.INFO, "{0} Worker [{1}] SmartProxy getFastestProxy returned NULL!", new Object[]{Thread.currentThread().getName(), _id});

                        URL url = new URL(chunk_url);

                        con = (HttpURLConnection) url.openConnection();
                    }

                } else {

                    URL url = new URL(chunk_url);

                    if (isUse_proxy()) {

                        _current_smart_proxy = null;

                        con = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getProxy_host(), getProxy_port())));

                        if (getProxy_user() != null && !"".equals(getProxy_user())) {

                            con.setRequestProperty("Proxy-Authorization", "Basic " + Bin2BASE64((getProxy_user() + ":" + getProxy_pass()).getBytes("UTF-8")));
                        }

                    } else {

                        con = (HttpURLConnection) url.openConnection();
                    }
                }

                if (_current_smart_proxy != null) {
                    con.setConnectTimeout(Download.HTTP_PROXY_TIMEOUT);
                    con.setReadTimeout(Download.HTTP_PROXY_TIMEOUT);
                } else {
                    con.setConnectTimeout(Download.HTTP_TIMEOUT);
                    con.setReadTimeout(Download.HTTP_TIMEOUT);
                }

                con.setUseCaches(false);

                con.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);

                long chunk_reads = 0;

                chunk_error = true;

                timeout = false;

                slow_proxy = false;

                File tmp_chunk_file = null, chunk_file = null;

                LOG.log(Level.INFO, "{0} Worker [{1}] is downloading chunk [{2}]!", new Object[]{Thread.currentThread().getName(), _id, chunk_id});

                try {

                    if (!_exit && !_download.isStopped()) {

                        http_status = con.getResponseCode();

                        if (http_status != 200) {

                            LOG.log(Level.INFO, "{0} Worker [{1}] Failed chunk download : HTTP error code : {2}", new Object[]{Thread.currentThread().getName(), _id, http_status});

                            http_error = http_status;

                        } else {

                            chunk_file = new File(_download.getChunkmanager().getChunks_dir() + "/" + new File(_download.getFile_name()).getName() + ".chunk" + chunk_id);

                            if (!chunk_file.exists() || chunk_file.length() != chunk_size) {

                                tmp_chunk_file = new File(_download.getChunkmanager().getChunks_dir() + "/" + new File(_download.getFile_name()).getName() + ".chunk" + chunk_id + ".tmp");

                                try (InputStream is = new ThrottledInputStream(con.getInputStream(), _download.getMain_panel().getStream_supervisor()); FileOutputStream tmp_chunk_file_os = new FileOutputStream(tmp_chunk_file)) {

                                    init_chunk_time = System.currentTimeMillis();

                                    paused = 0L;

                                    int reads = 0;

                                    byte[] buffer = new byte[DEFAULT_BYTE_BUFFER_SIZE];

                                    while (!_exit && !_download.isStopped() && !_download.getChunkmanager().isExit() && chunk_reads < chunk_size && (reads = is.read(buffer, 0, Math.min((int) (chunk_size - chunk_reads), buffer.length))) != -1) {

                                        tmp_chunk_file_os.write(buffer, 0, reads);

                                        chunk_reads += reads;

                                        _download.getPartialProgress().add((long) reads);

                                        _download.getProgress_meter().secureNotify();

                                        if (_download.isPaused() && !_download.isStopped()) {

                                            _download.pause_worker();

                                            pause_init_time = System.currentTimeMillis();

                                            secureWait();

                                            paused += System.currentTimeMillis() - pause_init_time;

                                        }

                                    }

                                    finish_chunk_time = System.currentTimeMillis();
                                }

                            } else {

                                LOG.log(Level.INFO, "{0} Worker [{1}] has RECOVERED PREVIOUS chunk [{2}]!", new Object[]{Thread.currentThread().getName(), _id, chunk_id});

                                finish_chunk_time = -1;

                                chunk_reads = chunk_size;

                                _download.getPartialProgress().add(chunk_size);

                                _download.getProgress_meter().secureNotify();
                            }

                        }

                        if (chunk_reads == chunk_size) {

                            LOG.log(Level.INFO, "{0} Worker [{1}] has DOWNLOADED chunk [{2}]!", new Object[]{Thread.currentThread().getName(), _id, chunk_id});

                            if (tmp_chunk_file != null && chunk_file != null && (!chunk_file.exists() || chunk_file.length() != chunk_size)) {

                                if (chunk_file.exists()) {
                                    chunk_file.delete();
                                }

                                tmp_chunk_file.renameTo(chunk_file);
                            }

                            chunk_error = false;

                            http_error = 0;

                            if (_current_smart_proxy != null && finish_chunk_time != -1) {

                                //Update average chunk download speed using SmartProxy
                                long chunk_speed = Math.round(chunk_size / (((double) (finish_chunk_time - init_chunk_time - paused)) / 1000));

                                _download.getMain_panel().getGlobal_dl_speed().update_avg_chunk_speed(chunk_speed);

                                long avg_chunk_speed = _download.getMain_panel().getGlobal_dl_speed().getAvg_chunk_speed();

                                if (avg_chunk_speed != -1) {
                                    //Proxy speed benchmark

                                    if (chunk_speed < Math.round(avg_chunk_speed * SLOW_PROXY_PERC)) {

                                        LOG.log(Level.INFO, "{0} Worker [{1}] WARNING -> PROXY {2} CHUNK DOWNLOAD SPEED: {3}/s SEEMS TO BE SLOW (average is {4}/s)", new Object[]{Thread.currentThread().getName(), _id, _current_smart_proxy, formatBytes(chunk_speed), formatBytes(avg_chunk_speed)});

                                        slow_proxy = true;
                                    }
                                }
                            }

                            if (!FORCE_SMART_PROXY) {
                                _current_smart_proxy = null;
                            }

                            _download.getChunkmanager().secureNotify();
                        }
                    }

                } catch (IOException ex) {

                    if (ex instanceof SocketTimeoutException) {
                        timeout = true;
                    }

                    LOG.log(Level.SEVERE, null, ex.getMessage());

                } finally {

                    if (chunk_error) {

                        if (!_exit && !_download.isStopped()) {

                            LOG.log(Level.INFO, "{0} Worker [{1}] has FAILED downloading chunk [{2}]!", new Object[]{Thread.currentThread().getName(), _id, chunk_id});

                        }

                        if (tmp_chunk_file != null && tmp_chunk_file.exists()) {
                            tmp_chunk_file.delete();
                        }

                        _download.rejectChunkId(chunk_id);

                        if (chunk_reads > 0) {
                            _download.getPartialProgress().add(-1 * chunk_reads);
                            _download.getProgress_meter().secureNotify();
                        }

                        if (!_exit && !_download.isStopped() && !timeout && (http_error != 509 || _current_smart_proxy != null) && http_error != 403) {

                            _error_wait = true;

                            _download.getView().updateSlotsStatus();

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException excep) {
                            }

                            _error_wait = false;

                            _download.getView().updateSlotsStatus();
                        }
                    }

                    con.disconnect();
                }
            }

        } catch (ChunkInvalidException e) {

        } catch (OutOfMemoryError | Exception error) {

            _download.stopDownloader(error.getMessage());

            LOG.log(Level.SEVERE, null, error.getMessage());

        }

        _download.stopThisSlot(this);

        _download.getChunkmanager().secureNotify();

        LOG.log(Level.INFO, "{0} Worker [{1}]: bye bye", new Object[]{Thread.currentThread().getName(), _id});
    }
    private static final Logger LOG = Logger.getLogger(ChunkDownloader.class.getName());
}
