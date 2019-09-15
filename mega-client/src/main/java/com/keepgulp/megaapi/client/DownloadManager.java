package com.keepgulp.megaapi.client;

import static com.keepgulp.megaapi.client.DBTools.*;
import static com.keepgulp.megaapi.client.MainPanel.*;
import static com.keepgulp.megaapi.client.MiscTools.*;
import java.awt.Component;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class DownloadManager extends TransferenceManager {

    public DownloadManager(MainPanel main_panel) {

        super(main_panel, main_panel.getMax_dl(), main_panel.getView().getStatus_down_label(), main_panel.getView().getjPanel_scroll_down(), main_panel.getView().getClose_all_finished_down_button(), main_panel.getView().getPause_all_down_button(), main_panel.getView().getClean_all_down_menu());
    }

    @Override
    public void remove(Transference[] downloads) {

        ArrayList<String> delete_down = new ArrayList<>();

        for (final Transference d : downloads) {

            swingInvoke(
                    new Runnable() {
                @Override
                public void run() {
                    getScroll_panel().remove(((Download) d).getView());
                }
            });

            getTransference_waitstart_queue().remove(d);

            if (getTransference_waitstart_queue().isEmpty()) {
                _frozen = false;
            }

            getTransference_running_list().remove(d);

            getTransference_finished_queue().remove(d);

            if (((Download) d).isProvision_ok()) {

                increment_total_size(-1 * d.getFile_size());

                increment_total_progress(-1 * d.getProgress());

                delete_down.add(((Download) d).getUrl());
            }
        }

        try {
            deleteDownloads(delete_down.toArray(new String[delete_down.size()]));
        } catch (SQLException ex) {
            LOG.log(SEVERE, null, ex);
        }

        secureNotify();
    }

    @Override
    public void provision(final Transference download) {
        swingInvoke(
                new Runnable() {
            @Override
            public void run() {
                getScroll_panel().add(((Download) download).getView());
            }
        });

        try {

            _provision((Download) download, false);

            secureNotify();

        } catch (APIException ex) {

            LOG.log(Level.INFO, "{0} Provision failed! Retrying in separated thread...", Thread.currentThread().getName());

            THREAD_POOL.execute(new Runnable() {
                @Override
                public void run() {

                    Thread.currentThread().setPriority(Math.max(Thread.currentThread().getPriority() - 1, Thread.MIN_PRIORITY));

                    try {

                        _provision((Download) download, true);

                    } catch (APIException ex1) {

                        LOG.log(SEVERE, null, ex1);
                    }

                    secureNotify();

                }
            });
        }

    }

    private void _provision(Download download, boolean retry) throws APIException {

        download.provisionIt(retry);

        if (download.isProvision_ok()) {

            increment_total_size(download.getFile_size());

            getTransference_waitstart_queue().add(download);

            synchronized (getQueue_sort_lock()) {

                if (!isPreprocessing_transferences() && !isProvisioning_transferences()) {

                    sortTransferenceWaitStartQueue();

                    swingInvoke(
                            new Runnable() {
                        @Override
                        public void run() {

                            for (final Transference down : getTransference_waitstart_queue()) {

                                getScroll_panel().remove((Component) down.getView());
                                getScroll_panel().add((Component) down.getView());
                            }

                            for (final Transference down : getTransference_finished_queue()) {

                                getScroll_panel().remove((Component) down.getView());
                                getScroll_panel().add((Component) down.getView());
                            }

                        }
                    });
                }
            }

        } else {

            getTransference_finished_queue().add(download);
        }
    }
    private static final Logger LOG = Logger.getLogger(DownloadManager.class.getName());

}
