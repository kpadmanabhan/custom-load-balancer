# custom-load-balancer
A custom load balancer approach.<br><br>
This is a sample written as a java console application as instructions specifically mention NOT to use any frameworks. A real-world-like separation would be to have these as individual springboot applications managed using a dependency management tool like maven or gradle. 

## Code walk-through
Steps (except Step 1) are coded within the [CustomLoadBalancer class](src/main/CustomLoadBalancer.java) sequentially. 
Enough comments written inside to explain the implementation and its rationale. 
Recommend doing a code walk-through for analyzing the solution.

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