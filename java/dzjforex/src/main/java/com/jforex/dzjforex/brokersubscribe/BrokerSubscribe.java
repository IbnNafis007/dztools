package com.jforex.dzjforex.brokersubscribe;

import java.util.Set;

import com.dukascopy.api.ICurrency;
import com.dukascopy.api.Instrument;
import com.jforex.dzjforex.brokeraccount.AccountInfo;
import com.jforex.dzjforex.config.ZorroReturnValues;
import com.jforex.dzjforex.misc.RxUtility;
import com.jforex.dzjforex.misc.Subscription;
import com.jforex.programming.currency.CurrencyFactory;
import com.jforex.programming.currency.CurrencyUtil;
import com.jforex.programming.instrument.InstrumentFactory;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public class BrokerSubscribe {

    private final Subscription subscription;
    private final AccountInfo accountInfo;

    public BrokerSubscribe(final Subscription subscription,
                           final AccountInfo accountInfo) {
        this.subscription = subscription;
        this.accountInfo = accountInfo;
    }

    public Single<Integer> forName(final String assetName) {
        return Single
            .defer(() -> RxUtility.instrumentFromName(assetName))
            .filter(instrument -> !subscription.isSubscribed(instrument))
            .flatMapCompletable(this::subscribe)
            .toSingleDefault(ZorroReturnValues.ASSET_AVAILABLE.getValue())
            .onErrorReturnItem(ZorroReturnValues.ASSET_UNAVAILABLE.getValue());
    }

    private Completable subscribe(final Instrument instrumentToSubscribe) {
        return Observable
            .just(instrumentToSubscribe)
            .mergeWith(crossInstruments(instrumentToSubscribe))
            .toList()
            .flatMapCompletable(subscription::set);
    }

    private Observable<Instrument> crossInstruments(final Instrument instrumentToSubscribe) {
        return Single
            .just(accountInfo.currency())
            .filter(accountCurrency -> !CurrencyUtil.isInInstrument(accountCurrency, instrumentToSubscribe))
            .flatMapObservable(accountCurrency -> {
                final Set<ICurrency> crossCurrencies = CurrencyFactory.fromInstrument(instrumentToSubscribe);
                return Observable.fromIterable(InstrumentFactory.combineWithAnchorCurrency(accountCurrency,
                                                                                           crossCurrencies));
            });
    }
}
