package com.keepgulp.megaapi.client;

import static com.keepgulp.megaapi.client.MainPanel.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public class StreamChunkDownloader implements Runnable {

    private final int _id;
    private final StreamChunkManager _chunkmanager;
    private volatile boolean _exit;

    public StreamChunkDownloader(int id, StreamChunkManager chunkmanager) {
        _id = id;
        _chunkmanager = chunkmanager;
        _exit = false;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    @Override
    public void run() {

        LOG.log(Level.INFO, "{0} Worker [{1}]: let''s do some work!", new Object[]{Thread.currentThread().getName(), _id});

        HttpURLConnection con = null;

        try {

            String url = _chunkmanager.getUrl();

            int http_error = 0;

            String current_smart_proxy = null;

            long offset = -1;

            SmartMegaProxyManager proxy_manager = getProxy_manager();

            while (!_exit && !_chunkmanager.isExit()) {

                while (!_exit && !_chunkmanager.isExit() && _chunkmanager.getChunk_queue().size() >= StreamChunkManager.BUFFER_CHUNKS_SIZE) {

                    LOG.log(Level.INFO, "{0} Worker [{1}]: Chunk buffer is full. I pause myself.", new Object[]{Thread.currentThread().getName(), _id});

                    _chunkmanager.secureWait();
                }

                if (http_error == 0) {

                    offset = _chunkmanager.nextOffset();

                } else if (http_error == 403) {

                    url = _chunkmanager.getUrl();
                }

                if (offset >= 0) {

                    StreamChunk chunk_stream = new StreamChunk(offset, _chunkmanager.calculateChunkSize(offset), url);

                    if (http_error == 509 && isUse_smart_proxy() && !isUse_proxy()) {

                        if (current_smart_proxy != null) {

                            proxy_manager.blockProxy(current_smart_proxy);
                        }

                        current_smart_proxy = proxy_manager.getFastestProxy();

                        if (current_smart_proxy != null) {

                            String[] proxy_info = current_smart_proxy.split(":");

                            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], Integer.parseInt(proxy_info[1])));

                            URL chunk_url = new URL(chunk_stream.getUrl());

                            con = (HttpURLConnection) chunk_url.openConnection(proxy);

                        } else {

                            URL chunk_url = new URL(chunk_stream.getUrl());

                            con = (HttpURLConnection) chunk_url.openConnection();
                        }

                    } else {

                        URL chunk_url = new URL(chunk_stream.getUrl());

                        if (isUse_proxy()) {

                            con = (HttpURLConnection) chunk_url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getProxy_host(), getProxy_port())));

                            if (getProxy_user() != null && !"".equals(getProxy_user())) {

                                con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((getProxy_user() + ":" + getProxy_pass()).getBytes("UTF-8")));
                            }
                        } else {

                            if (current_smart_proxy != null) {

                                String[] proxy_info = current_smart_proxy.split(":");

                                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], Integer.parseInt(proxy_info[1])));

                                con = (HttpURLConnection) chunk_url.openConnection(proxy);

                            } else {
                                con = (HttpURLConnection) chunk_url.openConnection();
                            }
                        }
                    }

                    con.setConnectTimeout(Transference.HTTP_TIMEOUT);

                    con.setReadTimeout(Transference.HTTP_TIMEOUT);

                    con.setUseCaches(false);

                    con.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);

                    int reads, http_status;

                    byte[] buffer = new byte[DEFAULT_BYTE_BUFFER_SIZE];

                    LOG.log(Level.INFO, "{0} Worker [{1}]: offset: {2} size: {3}", new Object[]{Thread.currentThread().getName(), _id, offset, chunk_stream.getSize()});

                    http_error = 0;

                    try {

                        if (!_exit) {

                            http_status = con.getResponseCode();

                            if (http_status != 200) {

                                LOG.log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                                http_error = http_status;

                            } else {

                                try (InputStream is = con.getInputStream()) {

                                    int chunk_writes = 0;

                                    while (!_exit && !_chunkmanager.isExit() && chunk_writes < chunk_stream.getSize() && (reads = is.read(buffer, 0, Math.min((int) (chunk_stream.getSize() - chunk_writes), buffer.length))) != -1) {

                                        chunk_stream.getOutputStream().write(buffer, 0, reads);

                                        chunk_writes += reads;
                                    }

                                    if (chunk_stream.getSize() == chunk_writes) {

                                        LOG.log(Level.INFO, "{0} Worker [{1}] has downloaded chunk [{2}]!", new Object[]{Thread.currentThread().getName(), _id, chunk_stream.getOffset()});

                                        _chunkmanager.getChunk_queue().put(chunk_stream.getOffset(), chunk_stream);

                                        _chunkmanager.secureNotifyAll();
                                    }
                                }
                            }
                        }

                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    } finally {
                        con.disconnect();
                    }

                } else {

                    _exit = true;
                }
            }

        } catch (IOException | URISyntaxException | ChunkInvalidException | InterruptedException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (OutOfMemoryError | Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        _chunkmanager.secureNotifyAll();

        LOG.log(Level.INFO, "{0} Worker [{1}]: bye bye", new Object[]{Thread.currentThread().getName(), _id});
    }
    private static final Logger LOG = Logger.getLogger(StreamChunkDownloader.class.getName());

}
