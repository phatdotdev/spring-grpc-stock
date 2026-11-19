package com.doph.service;

import com.doph.entity.Stock;
import com.doph.repository.StockRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;
import stocktrading.StockResponse;
import stocktrading.StockTradingServiceGrpc;

@GrpcService
@RequiredArgsConstructor
public class StockTradingServiceImpl extends StockTradingServiceGrpc.StockTradingServiceImplBase {

    private final StockRepository stockRepository;

    @Override
    public void getStockPrice(stocktrading.StockRequest request, StreamObserver<StockResponse> responseObserver) {
        // stockName -> DB -> map response -> return
        String stockSymbol = request.getStockSymbol();
        Stock stockEntity = stockRepository.findByStockSymbol(stockSymbol).orElse(null);

        StockResponse stockResponse = StockResponse.newBuilder()
                .setStockSymbol(stockSymbol)
                .setPrice(stockEntity.getPrice())
                .setTimestamp(stockEntity.getLastUpdated().toString())
                .build();
        responseObserver.onNext(stockResponse);
        responseObserver.onCompleted();
    }
}
