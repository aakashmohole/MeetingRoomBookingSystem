Viewed database.properties:1-5
Searched web: "spring 5 hibernate 5 local session factory bean xml configuration"

Yes! You can definitely use **Hibernate** (or JPA) in this project. 

Since this is a traditional Spring MVC project (not Spring Boot), you configure Hibernate by declaring a **SessionFactory** and **Transaction Manager** in your Spring XML file, and mapping your models with standard JPA annotations.

Here is the step-by-step guide on how to integrate and use Hibernate in this project:

---

### Step 1: Add Maven Dependencies
Open your **[pom.xml](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/pom.xml)** and add the following dependencies under `<dependencies>`:

```xml
<!-- Spring ORM (Bridges Spring Transactions and Hibernate SessionFactory) -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-orm</artifactId>
    <version>${spring.version}</version>
</dependency>

<!-- Hibernate Core -->
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>5.6.15.Final</version> <!-- Compatible with Spring 5 -->
</dependency>
```

---

### Step 2: Annotate your Models (JPA Annotations)
Open your model classes (like **[Room.java](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/java/com/meetingroom/model/Room.java)**) and map them to database tables:

```java
package com.meetingroom.model;

import javax.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "location", nullable = false)
    private String location;

    // Constructors, Getters, and Setters...
}
```

---

### Step 3: Configure `spring-servlet.xml`
Instead of using `JdbcTemplate` directly, configure the Hibernate `SessionFactory` and the `TransactionManager`. Open **[spring-servlet.xml](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/webapp/WEB-INF/spring-servlet.xml)** and add:

1. **Transaction XML Schema namespaces** to the `<beans>` root element:
   ```xml
   xmlns:tx="http://www.springframework.org/schema/tx"
   xsi:schemaLocation="...
       http://www.springframework.org/schema/tx
       https://www.springframework.org/schema/tx/spring-tx.xsd"
   ```

2. **Hibernate SessionFactory & Transaction Bean Definitions**:
   ```xml
   <!-- Hibernate SessionFactory (Replaces JdbcTemplate bean definition) -->
   <bean id="sessionFactory" class="org.springframework.orm.hibernate5.LocalSessionFactoryBean">
       <property name="dataSource" ref="dataSource"/>
       <!-- Automatically scans the model package for @Entity classes -->
       <property name="packagesToScan" value="com.meetingroom.model"/>
       <property name="hibernateProperties">
           <props>
               <!-- Adjust Dialect for your MySQL version -->
               <prop key="hibernate.dialect">org.hibernate.dialect.MySQL8Dialect</prop>
               <prop key="hibernate.show_sql">true</prop>
               <prop key="hibernate.format_sql">true</prop>
               <!-- Let Hibernate update tables automatically if schema changes -->
               <prop key="hibernate.hbm2ddl.auto">update</prop> 
           </props>
       </property>
   </bean>

   <!-- Hibernate Transaction Manager (Essential for Hibernate sessions) -->
   <bean id="transactionManager" class="org.springframework.orm.hibernate5.HibernateTransactionManager">
       <property name="sessionFactory" ref="sessionFactory"/>
   </bean>

   <!-- Enables declarative transaction management via @Transactional -->
   <tx:annotation-driven transaction-manager="transactionManager"/>
   ```

*(Note: You can remove the `<bean id="jdbcTemplate" ...>` definition since it is no longer required).*

---

### Step 4: Implement DAO using SessionFactory
Rewrite or create your DAOs to inject `SessionFactory` instead of `JdbcTemplate`:

```java
package com.meetingroom.dao;

import com.meetingroom.model.Room;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class HibernateRoomDao implements RoomDao {
    private final SessionFactory sessionFactory;

    public HibernateRoomDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Room> findAll() {
        // Query database using HQL (Hibernate Query Language)
        return sessionFactory.getCurrentSession()
                .createQuery("from Room", Room.class)
                .list();
    }
}
```

---

### Step 5: Enable Transactions
Since Hibernate requires active transactions to access database sessions via `sessionFactory.getCurrentSession()`, make sure to annotate your DAOs or your Services with `@Transactional` from Spring:

```java
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional // Automatically starts and commits transaction for DAO methods
public class HibernateRoomDao implements RoomDao {
    // ...
}
```