# custom-load-balancer
A custom load balancer approach.<br>

This is a sample written as a java console application as instructions specifically mention NOT to use any frameworks. A real-world-like separation would be to have these as individual springboot applications with dependency management using maven or gradle.<br>

All output is written to console for simplicity purposes. In real world, rotating logs to log files using logback or log4j based logging will be ideal.

## Evaluation
* Highly recommend doing a code walk-through for evaluation.
* Steps (except Step 1) are coded within the [CustomLoadBalancer class](src/main/CustomLoadBalancer.java) sequentially. 
* Enough comments written inside to explain the implementation and its rationale.
* After or while execution, trace calls by searching for the provider id in the console.

## Execution
* Clone the repo to local
* For executing from IDE 
  * Open solution in any IDE and execute it as a java application. Output is written to console.
* For executing from terminal
  * Run the below commands to compile and execute 
```
        $ javac -d out -cp out src/main/*.java src/main/providers/*.java src/main/loadbalancer/*.java
        $ java -cp out main.CustomLoadBalancer
```

## Notes
* [Load balancer](src/main/loadbalancer/LoadBalancer.java) is a singleton implementation
* All providers (abstracted through [IProvider interface](src/main/providers/IProvider.java)) register to [Load balancer](src/main/loadbalancer/LoadBalancer.java) with their unique ID