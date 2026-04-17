## DiscoveryPlugin (gRPC)
Позволяет динамически разрешать адрес базы данных через внешний сервис.<br>
Для активации плагина DiscoveryPlugin необходимо указать в JDBC URL префикс `jdbc:humus:grpc` и указать адрес gRPC-сервиса.

- **URL формат:** `jdbc:humus:grpc://discovery-service:9090/db_name`

<p>Топология кластера PostgreSQL:</p>

<table style="font-family: 'Courier New', monospace;">
  <tr>
    <th colspan="1"></th>
    <th colspan="1">Region 0</th>
    <th colspan="1">Region 1</th>
    <th colspan="1">Region 2</th>
  </tr>
  <tr>
    <td rowspan="5">Cluster 1</td>
    <td style="text-align: center; vertical-align: middle;">host0 (SYNC)</td>
    <td style="text-align: center; vertical-align: middle;">host1 (SYNC)<br>host2 (SYNC)<br>host3 (ASYNC)<br>host4 (ASYNC)<br>host5 (ASYNC)</td>
    <td style="text-align: center; vertical-align: middle;">host6 (ASYNC)<br>host7 (ASYNC)</td>
  </tr>
</table>

<table style="font-family: 'Courier New', monospace;">
  <tr>
    <th>wl_type</th>
    <th>reg_db</th>
    <th>reg_app</th>
    <th>jdbc:humus:grpc://discovery-service:9090/db_name</th>
    <th>формируемый jdbc uri</th>
  </tr>
  <tr>
    <td style="text-align: center; vertical-align: middle;"></td>
    <td style="text-align: center; vertical-align: middle;"></td>
    <td style="text-align: center; vertical-align: middle;"></td>
    <td></td>
    <td>jdbc:postgresql://host0,host1,host2,host3,host4,host5,host6,host7/db_name</td>
  </tr>
  <tr>
    <td style="text-align: center; vertical-align: middle;">rw</td>
    <td style="text-align: center; vertical-align: middle;"></td>
    <td style="text-align: center; vertical-align: middle;"></td>
    <td>?humus.wl_type=rw</td>
    <td>jdbc:postgresql://host0,host1,host2,host3,host4,host5,host6,host7/db_name</td>
  </tr>
  <tr>
    <td style="text-align: center; vertical-align: middle;"></td>
    <td style="text-align: center; vertical-align: middle;"></td>
    <td style="text-align: center; vertical-align: middle;"></td>
    <td>?targetServerType=primary</td>
    <td>jdbc:postgresql://host0,host1,host2,host3,host4,host5,host6,host7/db_name</td>
  </tr>
  <tr>
    <td style="text-align: center; vertical-align: middle;"></td>
    <td style="text-align: center; vertical-align: middle;">r0 r1</td>
    <td style="text-align: center; vertical-align: middle;"></td>
    <td>?targetServerType=primary&humus.reg_db=r1,r2</td>
    <td>jdbc:postgresql://host0,host1,host2/db_name</td>
  </tr>
</table>
