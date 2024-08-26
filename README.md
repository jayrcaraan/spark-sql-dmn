# Spark SQL DMN

This is an Apache Spark plugin to include a light support for Decision Model and Notation (DMN) objects.  
The goal is to reduce the complexity SQL users by providing an interface to call DMNs as if they are just SQL functions in Spark.

Example:
```sql
SELECT 
  customer_id,
  credit_score,
  loan_amount,
  evaluate_decision_table(
        struct(credit_score, loan_amount), 
        'loan_application.xml') AS decision
FROM loan_entry
```

This should return an object with this structure:
```text
root
 |-- customer_id: integer (nullable = false)
 |-- credit_score: float (nullable = true)
 |-- loan_amount: integer (nullable = false)
 |-- decision: struct (nullable = false)
 |    |-- loan_status: string (nullable = true)

```

An example output would be:
```text
+-----------+------------+-----------+----------+
|customer_id|credit_score|loan_amount|decision  |
+-----------+------------+-----------+----------+
|100        |0.8         |10000      |{approved}|
|101        |0.7         |10000      |{declined}|
+-----------+------------+-----------+----------+
```
---
## Usage:
1. Include the library to the Spark runtime through the Spark Config - `spark.jars` or `spark.jars.packages` if you're using your own artifact repository.
2. Register `org.apache.spark.sql.dmn.DmnSqlExtensions` as SQL Extension (`spark.sql.extensions`) through the Spark Config.  
3. The function accepts two arguments:
    ```text
    evaluate_decision_table(<data>, <dmn path>)
    - data - a named struct containing the input that are required by the DMN object
    - dmn - the location of the DMN xml file
    ```
   Since the library is currently using a standard filesystem library, it is recommended to distribute the DMN XMLs as Spark Files `spark.files`.  
   This should be straightforward enough if you're planning on using this inside a Databricks environment.  
   Ideally, you should be able to just provide the path of the file in the volume.
