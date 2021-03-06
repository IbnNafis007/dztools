package com.jforex.dzjforex.brokertime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jforex.dzjforex.brokertime.ntp.NTPProvider;
import com.jforex.dzjforex.brokertime.ticktime.TickTimeProvider;

import io.reactivex.Single;

public class ServerTimeProvider {

    private final NTPProvider ntpProvider;
    private final TickTimeProvider tickTimeProvider;

    private final static Logger logger = LogManager.getLogger(ServerTimeProvider.class);

    public ServerTimeProvider(final NTPProvider ntpProvider,
                              final TickTimeProvider tickTimeProvider) {
        this.ntpProvider = ntpProvider;
        this.tickTimeProvider = tickTimeProvider;
    }

    public Single<Long> get() {
        return Single
            .defer(ntpProvider::get)
            .onErrorResumeNext(serverTimeFromTick());
    }

    private Single<Long> serverTimeFromTick() {
        return Single.defer(() -> {
            logger.warn("Currently no NTP available, estimating with latest tick time...");
            return tickTimeProvider.get();
        });
    }
}
