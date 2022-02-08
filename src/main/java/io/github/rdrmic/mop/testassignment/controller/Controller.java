package io.github.rdrmic.mop.testassignment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.rdrmic.mop.testassignment.mock.MockDataController;
import io.github.rdrmic.mop.testassignment.service.PayloadService;
import io.github.rdrmic.mop.testassignment.service.RequestExecutionTimeService;
import io.github.rdrmic.mop.testassignment.util.ColorLogger;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("mop-test-assignment")
public class Controller {
	
	private static final ColorLogger LOG = new ColorLogger(Controller.class);
	
	@Autowired
    private PayloadService payloadService;
	
	@Autowired
    private RequestExecutionTimeService requestExecutionTimeService;
	
	@Autowired
	private Environment environment;
	
	@GetMapping(value ="execute")
	private Flux<?> execute() {
		if ("mock".equals(environment.getActiveProfiles()[0])) {
			MockDataController.reset();
		}

		LOG.info("STARTING execute() ...");
		return payloadService.execute();
	}
	
	@GetMapping("response-time-averages")
	private Flux<?> responseTimeAverages() {
        return requestExecutionTimeService.getTodaysAverages();
    }

}
