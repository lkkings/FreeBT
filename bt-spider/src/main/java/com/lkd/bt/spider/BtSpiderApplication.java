package com.lkd.bt.spider;


import com.lkd.bt.spider.task.*;
import lombok.RequiredArgsConstructor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
@MapperScan("com.lkd.bt.spider.mapper")
@RequiredArgsConstructor
public class BtSpiderApplication implements CommandLineRunner {

	private final List<Task> tasks;

	public static void main(String[] args) {
		SpringApplication.run(BtSpiderApplication.class, args);
	}

	@Override
	public void run(String... args){
		tasks.forEach(Task::execute);
	}
}
