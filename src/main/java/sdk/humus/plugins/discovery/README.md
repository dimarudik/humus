## DiscoveryPlugin (gRPC)
Позволяет динамически разрешать адрес базы данных через внешний сервис.<br>
Для активации плагина DiscoveryPlugin необходимо указать в JDBC URL префикс `jdbc:humus:grpc` и указать адрес gRPC-сервиса.

- **URL формат:** `jdbc:humus:grpc://discovery-service-host:9090/db_name`


<table>
  <tr>
    <th colspan="2">Заголовок на две колонки</th>
    <th colspan="1">jdbc uri</th>
  </tr>
  <tr>
    <td style="text-align: center; vertical-align: middle;">Ячейка 1</td>
    <td>Ячейка 2</td>
    <td>jdbc:postgresql://host1:5432,hots2:5432,hots2:5432/db_name</td>
  </tr>
  <tr>
    <td>Влево</td>
    <td>Ячейка 2</td>
    <td>Ячейка 2</td>
  </tr>
</table>
