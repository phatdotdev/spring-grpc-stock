package com.doph;

import com.doph.service.StockClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class StockTradingClientApplication implements CommandLineRunner {

	private final StockClientService stockClientService;

	public static void main(String[] args) {
		SpringApplication.run(StockTradingClientApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

//		System.out.println("GRPC client response: "
//				+ stockClientService.getStockPrice("VCB")
//		);
//		stockClientService.subscribeStockPrice("VCB");
//		stockClientService.placeBulkOrders();
		stockClientService.startLiveTrading();
	}
}
