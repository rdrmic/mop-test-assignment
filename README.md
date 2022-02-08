#### Preparing the database
There is a script 'ddl.sql' in project root for creating DB / tables. Connection string, username and password are in *application.properties*.

#### Running the application
There are 2 profiles in project: 'dev' (default) and 'mock'. And since Maven is already included in project, they should be run with
- mvnw spring-boot:run
- mvnw spring-boot:run -Pmock

Difference is that 'mock' itself serves a small quantity of data via *MockController* (in the *mock* package), and by calling 'Task 1's endpoint could be seen how response timeouts are handled, because the mocked endpoint creates delays, and on 2 endpoints repeatedly returns HTTP 408 response statuses.

Default profile fetches data from endpoints listed in the specification.

#### Endpoints
- **Task 1:** *GET* [http://localhost:8081/mop-test-assignment/data](http://localhost:8081/mop-test-assignment/data)
- **Task 2:** *GET* [http://localhost:8081/mop-test-assignment/response-time-averages](http://localhost:8081/mop-test-assignment/response-time-averages)

#### Console logging
There are 2 switches (on/off) in *application.properties* that toggle *WebFlux's* internal logging: 'log.webclient' and 'log.r2dbc'.
They are initialy off, but they could be switched on to see more clearly what is happening regarding threads, asynchronicity and non-blocking nature of the implementation.

#### Error handling and transaction management
Besides HTTP 408 status, I didn't handled other eventual exceptions or HTTP statuses different than 200. So it CAN happen that some of the request execution times get persisted in those cases, but that can be fixed quickly.
