## 🚀 DiscoveryPlugin (gRPC)
Позволяет динамически разрешать адрес базы данных через внешний сервис.<br>
Для активации плагина DiscoveryPlugin необходимо указать в JDBC URL префикс `jdbc:humus:grpc` и указать адрес gRPC-сервиса.

- **URL формат:** `jdbc:humus:grpc://discovery-service:9090/db_name`

### 🛠 Параметры & фильтры:
- **humus.instance** - имя кластера (обязательное поле)
- **humus.wl_type** - тип нагрузки: ro - только чтение, rw - чтение-запись. (default: rw)
- **humus.reg_db** - список регионов, в которых находятся хосты кластера PostgreSQL. (default: все регионы) 
- **humus.reg_app** - регион, в котором находится хост приложения. В формируемом uri, первыми будут отдаваться хосты кластера ближайших регионов.

#### ✔ Логика фильтров: 

`humus.wl_type & humus.reg_db` работают как фильтры, по которым отбираются хосты кластера

`humus.reg_app` определяет порядок хостов по расстоняию между регионами в формируемом uri и не является фильтром 


#### ✔ Топология кластера PostgreSQL:

Для простоты понимая, все регионы стоят на одной географической линии в порядке R0, R1, R2.

<table style="font-family: 'Courier New', monospace;">
  <tr>
    <th colspan="1"></th>
    <th colspan="1">Region 0</th>
    <th colspan="1">Region 1</th>
    <th colspan="1">Region 2</th>
  </tr>
  <tr style="text-align: center; vertical-align: middle;">
    <td rowspan="5">Cluster 1</td>
    <td>host0 (SYNC)</td>
    <td>host1 (SYNC)<br>host2 (SYNC)<br>host3 (ASYNC)<br>host4 (ASYNC)<br>host5 (ASYNC)</td>
    <td>host6 (ASYNC)<br>host7 (ASYNC)</td>
  </tr>
</table>

#### ✔ Формируемый JDBC URL:
<table style="font-family: 'Courier New', monospace;">
  <tr style="text-align: center; vertical-align: middle;">
    <th>wl_type</th>
    <th>reg_db</th>
    <th>reg_app</th>
    <th>jdbc:humus:grpc://discovery-service:9090/db_name?humus.instance=rda15&</th>
    <th>формируемый jdbc uri</th>
  </tr>
  <tr style="text-align: center; vertical-align: middle;">
    <td></td>
    <td></td>
    <td></td>
    <td></td>
    <td>jdbc:postgresql://host0,host1,host2/db_name</td>
  </tr>
  <tr style="text-align: center; vertical-align: middle;">
    <td>rw</td>
    <td></td>
    <td></td>
    <td>humus.wl_type=rw</td>
    <td>jdbc:postgresql://host0,host1,host2/db_name</td>
  </tr>
  <tr style="text-align: center; vertical-align: middle;">
    <td></td>
    <td></td>
    <td></td>
    <td>targetServerType=primary</td>
    <td>jdbc:postgresql://host0,host1,host2/db_name?targetServerType=primary</td>
  </tr>
  <tr style="text-align: center; vertical-align: middle;">
    <td></td>
    <td>r0 r1</td>
    <td></td>
    <td>targetServerType=primary&humus.reg_db=r0,r1</td>
    <td>jdbc:postgresql://host0,host1,host2/db_name?targetServerType=primary</td>
  </tr>
  <tr style="text-align: center; vertical-align: middle;">
    <td>ro</td>
    <td>r1 r2</td>
    <td></td>
    <td>humus.wl_type=ro&humus.reg_db=r1,r2</td>
    <td>jdbc:postgresql://host3,host4,host5,host6,host7/db_name</td>
  </tr>
  <tr style="text-align: center; vertical-align: middle;">
    <td>ro</td>
    <td>r1 r2</td>
    <td>r2</td>
    <td>humus.wl_type=ro&humus.reg_db=r1,r2&humus.reg_app=r2</td>
    <td>jdbc:postgresql://host6,host7,host3,host4,host5/db_name</td>
  </tr>
  <tr style="text-align: center; vertical-align: middle;">
    <td>ro</td>
    <td></td>
    <td>r0</td>
    <td>humus.wl_type=ro&humus.reg_app=r0</td>
    <td>jdbc:postgresql://host3,host4,host5,host6,host7/db_name</td>
  </tr>
</table>

Пользователь должен осознанно подходить к конфигурации фильтров. На стороне сервиса проверка корректности конфигурации не осуществляется. 