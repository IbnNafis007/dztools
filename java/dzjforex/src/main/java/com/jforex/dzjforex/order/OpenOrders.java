package com.jforex.dzjforex.order;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dukascopy.api.IEngine;
import com.dukascopy.api.IOrder;
import com.jforex.dzjforex.config.PluginConfig;
import com.jforex.dzjforex.misc.RxUtility;

import io.reactivex.Maybe;
import io.reactivex.Single;

public class OpenOrders {

    private final IEngine engine;
    private final OrderRepository orderRepository;
    private final PluginConfig pluginConfig;

    private final static Logger logger = LogManager.getLogger(OpenOrders.class);

    public OpenOrders(final IEngine engine,
                      final OrderRepository orderRepository,
                      final PluginConfig pluginConfig) {
        this.engine = engine;
        this.orderRepository = orderRepository;
        this.pluginConfig = pluginConfig;
    }

    public Maybe<IOrder> getByID(final int orderID) {
        return Single
            .defer(this::ordersFromEngine)
            .flatMapCompletable(orderRepository::store)
            .andThen(Maybe.defer(() -> orderRepository.getByID(orderID)));
    }

    private Single<List<IOrder>> ordersFromEngine() {
        return Single
            .fromCallable(engine::getOrders)
            .doOnSubscribe(d -> logger.debug("Fetching open orders..."))
            .doOnSuccess(orders -> logger.debug("Fetched " + orders.size() + " open orders."))
            .retryWhen(RxUtility.retryForHistory(pluginConfig))
            .doOnError(e -> logger.info("Fetching open orders failed! " + e.getMessage()));
    }
}
