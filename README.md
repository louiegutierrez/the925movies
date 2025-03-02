- # General
  - #### Team: The925

  - #### Names: Leonardo Gutierrez (leonarlg) && Martin Kimball (mkimbal1)

  - #### Project 5 Video Demo Link:

  - #### Instruction of deployment:
    - Accessible online through the925movies.shop/The925-1
    - Deployment locally steps:
      - mvn clean package in terminal
      - sudo cp ./target/*.war [PATH_TO_TOMCAT]
      - Then go to localhost:8080/The925-1

  - #### Collaborations and Work Distribution:
    - #### Martin Kimball:
      - Full-Text Implementation
      - Autocomplete Implementation
      - Video Uploading
    - #### Leonardo Gutierrez:
      - Implemented AWS Load Balancing
      - AWS Master/Slave Implementation
      - GCP Implementation

- # Connection Pooling
  - #### Include the filename/path of all code/configuration files in GitHub of using JDBC Connection Pooling.
    - src/main/webapp/META-INF/context.xml : Added two resources for Connection Pooling for master/slave
    - src/main/webapp/WEB-INF/web.xml : Added References to both resources
    - src/main/java/*Servlet.java : All Servlet files utilizes one of the two resources defined as moviedb_master or moviedb_slave

  - #### Explain how Connection Pooling is utilized in the Fabflix code.
    - Connection Pooling is utilized with two backend instances of SQL to offload and prevent overloading one instance
    - The Code uses this to redirect writes and reads to separate instances in all *Servlet.java files

  - #### Explain how Connection Pooling works with two backend SQL.
    - Connection Pooling works in our project by redirecting writes to the master and reads to the slave(s)
    - Then we have a load balancer that balances the work between the two equally


- # Master/Slave
  - #### Include the filename/path of all code/configuration files in GitHub of routing queries to Master/Slave SQL.
    - src/main/java/*Servlet.java : All Servlet files utilizes Master/Slave SQL databases
    - src/main/webapp/META-INF/context.xml : Added two resources for Connection Pooling for master/slave

  - #### How read/write requests were routed to Master/Slave SQL?
    - Read/Write requests were redirected via their respective resources defined as moviedb_master or moviedv_slave
    - Path to find those defined resources are above where the IPs are to SQL databases