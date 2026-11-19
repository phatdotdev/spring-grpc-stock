package com.doph.service;

import com.doph.entity.Stock;
import com.doph.grpc.StockRequest;
import com.doph.grpc.StockResponse;
import com.doph.grpc.StockTradingServiceGrpc;
import com.doph.repository.StockRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@GrpcService
@RequiredArgsConstructor
public class StockTradingServiceImpl extends StockTradingServiceGrpc.StockTradingServiceImplBase {

    private final StockRepository stockRepository;

    @Override
    public void getStockPrice(StockRequest request, StreamObserver<StockResponse> responseObserver) {
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

    @Override
    public void subscribeStockPrice(StockRequest request, StreamObserver<com.doph.grpc.StockResponse> responseObserver) {
        String stockSymbol = request.getStockSymbol();
        Stock stockEntity = stockRepository.findByStockSymbol(stockSymbol).orElse(null);
        try {
            for(int i = 0; i <= 10; i++) {
                StockResponse stockResponse = StockResponse.newBuilder()
                        .setStockSymbol(stockSymbol)
                        .setPrice(new Random().nextDouble(200))
                        .setTimestamp(Instant.now().toString())
                        .build();
                responseObserver.onNext(stockResponse);

                TimeUnit.SECONDS.sleep(1);
            }
            responseObserver.onCompleted();
        }catch(InterruptedException ex){
            responseObserver.onError(ex);
        }
    }

}
