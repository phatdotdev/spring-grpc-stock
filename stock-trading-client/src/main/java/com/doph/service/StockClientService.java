package com.doph.service;

import com.doph.grpc.StockRequest;
import com.doph.grpc.StockResponse;
import com.doph.grpc.StockTradingServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class StockClientService {

    @GrpcClient("stockService")
    private StockTradingServiceGrpc.StockTradingServiceBlockingStub serviceBlockingStub;

    @GrpcClient("stockService")
    private StockTradingServiceGrpc.StockTradingServiceStub asyncStub;

    // StockResponse getStockPrice(StockRequest)
    public StockResponse getStockPrice(String stockSymbol){
        StockRequest request = StockRequest.newBuilder().setStockSymbol(stockSymbol).build();
        return serviceBlockingStub.getStockPrice(request);
    }

    public void subscribeStockPrice(String stockSymbol){
        StockRequest request = StockRequest.newBuilder().setStockSymbol(stockSymbol).build();
        asyncStub.subscribeStockPrice(request, new StreamObserver<>(){

            @Override
            public void onNext(StockResponse stockResponse) {
                System.out.println("Stock Price Updated: "+stockResponse.getStockSymbol() +
                        " Price: " + stockResponse.getPrice() +
                        " Time: " + stockResponse.getTimestamp());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error: " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Stream completed");
            }
        });
    }
}
