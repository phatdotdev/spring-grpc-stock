package com.doph.service;

import com.doph.grpc.StockRequest;
import com.doph.grpc.StockResponse;
import com.doph.grpc.StockTradingServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class StockClientService {

    @GrpcClient("stockService")
    private StockTradingServiceGrpc.StockTradingServiceBlockingStub serviceBlockingStub;

    // StockResponse getStockPrice(StockRequest)
    public StockResponse getStockPrice(String stockSymbol){
        StockRequest request = StockRequest.newBuilder().setStockSymbol(stockSymbol).build();
        return serviceBlockingStub.getStockPrice(request);
    }
}
