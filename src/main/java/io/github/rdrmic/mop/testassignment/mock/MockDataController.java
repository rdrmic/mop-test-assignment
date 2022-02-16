package io.github.rdrmic.mop.testassignment.mock;

import java.sql.SQLException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("mop-test-assignment")
public class MockDataController {
	
	private static final String MOCK_DATA_1 = MockDataProvider.getString("DATA_1");
	private static final String MOCK_DATA_2 = MockDataProvider.getString("DATA_2");
	private static final String MOCK_DATA_3 = MockDataProvider.getString("DATA_3");
	private static final String MOCK_DATA_4 = MockDataProvider.getString("DATA_4");
	
	private static final boolean SIMULATE_DELAYS_AND_ERRORS = true;
	
	private static int response408data2count;
	private static int response408data4count;
	//private static int response503count;
	
	public static void reset() {
		response408data2count = 0;
		response408data4count = 0;
		//response503count = 0;
	}
	
	@GetMapping("api1")
	private ResponseEntity<String> mockData1() throws SQLException {
		if (SIMULATE_DELAYS_AND_ERRORS) {
			try {
				Thread.sleep(3004);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
        return ResponseEntity.ok(MOCK_DATA_1);
    }
	
	@GetMapping("api2")
	private ResponseEntity<String> mockData2() throws SQLException {
		ResponseEntity<String> response;
		if (SIMULATE_DELAYS_AND_ERRORS) {
			try {
				Thread.sleep(3003);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (++response408data2count <= 7) {
				response = new ResponseEntity<>(MOCK_DATA_2, HttpStatus.REQUEST_TIMEOUT);
			} else {
				response = ResponseEntity.ok(MOCK_DATA_2);
			}
		} else {
			response = ResponseEntity.ok(MOCK_DATA_2);
		}
        return response;
    }
	
	@GetMapping("api3")
	private ResponseEntity<String> mockData3() throws SQLException {
		if (SIMULATE_DELAYS_AND_ERRORS) {
			try {
				Thread.sleep(3002);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
        return ResponseEntity.ok(MOCK_DATA_3);
    }
	
	@GetMapping("api4")
	private ResponseEntity<String> mockData4() throws SQLException {
		ResponseEntity<String> response;
		if (SIMULATE_DELAYS_AND_ERRORS) {
			try {
				Thread.sleep(3001);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (++response408data4count <= 3) {
				response = new ResponseEntity<>(MOCK_DATA_4, HttpStatus.REQUEST_TIMEOUT);
			} else {
				response = ResponseEntity.ok(MOCK_DATA_4);
			}
		} else {
			response = ResponseEntity.ok(MOCK_DATA_4);
		}
        return response;
    }

}
