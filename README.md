# account-service

Microservicio que maneja los productos pasivos (cuentas bancarias) del banco.

- Ahorro: libre de comisión por mantenimiento y con un límite máximo de movimientos mensuales.
- Cuenta corriente: posee comisión de mantenimiento y sin límite de movimientos mensuales.
- Plazo fijo: libre de comisión por mantenimiento, solo permite un movimiento de retiro o depósito en un día específico del mes.

Un cliente puede hacer depósitos y retiros de sus cuentas bancarias.

- Las cuentas bancarias tienen un monto mínimo de apertura que puede ser cero (0).
- VIP: cuenta de ahorro que requiere un monto mínimo de promedio diario cada mes y tener una tarjeta de crédito con el banco.
- PYME: cuenta corriente sin comisión de mantenimiento, requiere tener una tarjeta de crédito con el banco.
- Todas las cuentas tienen un número máximo de transacciones sin comisión, superado ese número se cobra comisión por cada transacción.
- Transferencias entre cuentas del mismo cliente y cuentas a terceros del mismo banco.
- Reporte general por producto en un intervalo de tiempo y reporte de los últimos 10 movimientos.
- Tarjetas de débito asociadas a las cuentas bancarias para hacer pagos.
- Un cliente no podrá adquirir un producto si posee alguna deuda vencida en algún producto de crédito.

## Tecnologías

- Java 17
- Spring Boot
- Spring Cloud (Config, Eureka, Resilience4j)
- RxJava
- Kafka
- Maven
- MongoDB
- Lombok
- Logback
- JUnit, Mockito y JaCoCo
- Docker
