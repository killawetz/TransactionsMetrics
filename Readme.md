# Транзакции, Лабораторная №4 из курса "Базы данных", Отчёт #

## Цели работы ##
Для трех уровней изолированности транзакций:

* Read committed
* Repeatable read
* Serializable

реализовать тестовое приложение и поставить с его помощью следующий эксперимент: для БД с предварительно сгенерированным набором тестовых данных (генерация должна быть осуществлена с помощью приложения, реализованного в рамках работы №2) запустить 3 потока для работы с БД:

* поток №1 выполняет выборку данных из одной из таблиц БД
* поток №2 добавляет по одно записи в ту же таблицу БД
* поток №3 изменяет записи в той же таблице БД, условие для изменения должно затрагивать и данные потока №1 и запись, добавляемую потоком №2

каждый поток должен выполнить серию (нужно иметь возможность указать ее размер) запросов к БД, для каждого запроса должно быть измерено среднее время выполнения. Для каждого потока для каждого уровня изолированности необходимо вывести среднее время выполнения запроса.
Проанализировать полученные результаты и продемонстрировать преподавателю.

## Ход работы ##


### Уровни изолированности ###

Для начала необходимо разобраться, что же такое уровни изоляции транзакций. 
Имеем мы ровно 4 уровня изолированности, а именно:
* Read uncomitted 
* Read comitted
* Repeatable read
* Serializable 

Т.к. в курсе лабораторных работ я использую СУБД PostgreSQL стоит уточнить, что в postgres мы можем запросить любой из четырёх уровней изоляции транзакций,
однако внутри реализованы только три различных уровня, то есть режим Read Uncommitted действует как Read Committed.  
Немаловажным уточнением является факт, что Repeatable Read в PostgreSQL не допускает возможности фантомного чтения. (Ситуация, когда при повторном чтении в рамках одной транзакции одна и та же выборка дает разные множества строк).  
Первоначально такое утверждение вводит в заблуждение и размываются различия между уровнями Repeatable Read и Serializable, однако дальнейшее чтение документации развеивает все вопросы.
Следующее утверждение:  
>The Serializable isolation level provides the strictest transaction isolation.
 This level emulates serial transaction execution for all committed transactions; as if transactions had been executed one after another, serially, rather than concurrently.
 However, like the Repeatable Read level, applications using this level must be prepared to retry transactions due to serialization failures.
 In fact, this isolation level works exactly the same as Repeatable Read except that it monitors for conditions which could make execution of a concurrent set of serializable transactions behave in a manner inconsistent with all possible serial (one at a time) executions of those transactions.
 This monitoring does not introduce any blocking beyond that present in repeatable read, but there is some overhead to the monitoring, and detection of the conditions which could cause a serialization anomaly will trigger a serialization failure.  
 
Иными словами, уровень Serializable отличается от Repeatable Read защищенностью от "аномалии сериализации" - ситуации, когда параллельное выполнение транзакций приводит к результату, невозможному при последовательном выполнении тех же транзакций.  

Итоговая таблица характеристик уровней изоляции выглядит следующим образом:  

![Transaction Isolation Levels](Lab4/images/isolation_level_table.png)

***Грязное чтение/Dirty Read*** - Чтение данных, добавленных или изменённых еще не завершенной транзакцией.

***Неповторяющееся чтение/Nonrepeatable Read*** - При повторном чтении в рамках одной транзакции ранее прочитанные данные оказываются изменёнными.

***Чтение фантомов/Phantom Read*** - Ситуация, когда при повторном чтении в рамках одной транзакции одна и та же выборка дает разные множества строк.


### Запросы ###

Код моей программы находится в следующей директории: [Transaction Project](Lab4/TransactionsProject)  

Код проекта включает в себя уже известные классы [MovieGenerator.java](Lab4/TransactionsProject/src/main/java/Generator/MovieGenerator.java)  
и [DBConnector.java](Lab4/TransactionsProject/src/main/java/util/DBConnector.java) слегка видоизмененные для нужд текущего технического задания.  

Для реализации многопоточного обращения к БД был написан класс [TransactionThread.java](Lab4/TransactionsProject/src/main/java/TransactionThread.java)  

Логика приложения заключается в следующем:  
Синхронизированные по началу и окончанию времени работы 3 потока выполняют запросы INSERT, SELECT, UPDATE к моей БД. Каждый поток имеет свое соединение.
Параметрами запуска потоков являются **количество итераций** и **уровень изолированности**, где кол-во итераций - сколько раз будет выполнен каждый запрос на каждом уровне изоляции.

Перед сменной уровня изоляции транзакций база данных приводится в исходное состояние, а именно: 400 (параметр, можно изменить) уже имеющихся в таблице film строк.
Т.е. перед тем, как начать выполнять запросы и замерять результаты, база данных очищается от результатов предыдущего опыта и генерируются исходные данные (всегда один и тот же набор строк).  

Исходя из главного условия технического задания, а именно 
>поток №3 изменяет записи в той же таблице БД, условие для изменения должно затрагивать и данные потока №1 и запись, добавляемую потоком №2  

Были придуманы следующие запросы:  

**INSERT**:  
```sql
INSERT INTO public.film(name, description, budget, year, runtime) values(?, 'descr', 90210, ?, 123)
```
Поля *name* и *year* - подставляемые уникальные значения, чтоб избежать вставки повторяющихся строк. Бюджет у всех вставляемых фильмов - 90210.

**SELECT**:
```sql
SELECT * FROM film WHERE budget < 1000000
```
Выбираем только фильмы с бюджетом меньше 1000000 (миллиона).  

**UPDATE**:
```sql
UPDATE film SET runtime = 222 WHERE budget < 1000000
```
Изменяем поле runtime у всех фильмов с бюджетом меньше 1000000.

Строки получающиеся в результате выполнения серии этих запросов, откровенно говоря, не несут в себе никакой смысловой ценности и вряд ли могут быть применимы в каком-то "боевом" проекте, но в рамках текущей лабораторной работы со своей задачей они прекрасно справляются :).  
Немаловажно отметить, что из предварительно сгенерированных 400 строк - 28 с бюджетом меньше 1000000. Увеличение количества генерируемых строк так же увеличит количество строк с бюджетом < 1000000.


### Замеры ###

Метрики для каждого запроса на каждом уровне транзакций являются двух типов.  
**1-ый тип** -  время прошедшее от начала выполнения запросов до окончания серии.  
**2-ой тип** -  индивидуальное время выполнение каждого запроса.

Все собранные значения я сохраняю в формате json ([total_time_metrics](Lab4/TransactionsProject/src/main/data/metrics_total.json) и [average_time_metrics](Lab4/TransactionsProject/src/main/data/metrics_average.json))

Выглядит это все в следующем виде:  

![Json format](Lab4/images/json_format.png)


### Общее время серий запросов. Графики. ###  

Для начала посмотрим на 3 достаточно наглядных графика, на которых изображено сколько времени понадобилось каждой серии запросов на каждом уровне изоляции для выполнения.  
Ось Х - номера итерации (порядковый номер каждого запроса в серии)
Ось Y - время в миллисекундах от начала выполнения серии

Для уровня изоляции **READ COMMITTED** наблюдаем следующую картину:  
![total_read_committed](Lab4/images/total_read_committed.png)  

Для уровня изоляции **REPEATABLE READ**:  
![total_repeatable_read](Lab4/images/total_repeatable_read.png)  

А время за которое отработала серия запросов на уровне **SERIALIZE* *выглядит так:  
![total_serialize](Lab4/images/total_serialize.png)  

Я считаю эти графики достаточно наглядными, потому что:
* По ним легко можно определить самую быструю серию запросов и самую медленную ( самый быстрый - **INSERT**, самый медленный - **UPDATE** для всех уровней изоляции)
* Видно, как параллельные запросы влияют на производительность друг друга. Например, график **UPDATE** становится линейным (или более-менее похожим на него) только по завершению серии запросов **SELECT**  


### Среднее время выполнение запроса. Сравнение. Графики. ###  

Теперь следующая задача: сравнить среднее время выполнения запросов одного типа на разных уровнях изоляции.  
Мат.статистика и здравый смысл подсказывают мне, что я не могу делить общее время выполнения серий на их размер и сравнивать эти значения.  

 
Для начала посчитаем доверительные интервалы для трех типов запросов на разных уровнях изоляции и посмотрим не перекрываются ли они.    

Доверительные интервалы для запроса **INSERT**:  
____________________________________  
INSERT READ COMMITTED  
Mean value =  0.4300445  
Радиус = 0.043631618256289764  
Доверительный интервал RC = ( 0.38641288174371025 : 0.47367611825628975 )  
____________________________________  
INSERT REPEATABLE READ  
Mean value =  0.3428829  
Радиус = 0.017205370130080576  
Доверительный интервал RR = ( 0.32567752986991944 : 0.36008827013008055 )  
____________________________________  
INSERT SERIALIZE  
Mean value =  0.6251087  
Радиус = 0.14853958943376683  
Доверительный интервал S = ( 0.47656911056623313 : 0.7736482894337668 )  
____________________________________  

Графическое отображение:  

![confidence interval insert](Lab4/images/ci_insert.png)  

Как видим, доверительные интервалы не перекрываются, значит разница является статистически значимой.  

Доверительные интервалы для запроса **SELECT**:  
____________________________________  
SELECT READ COMMITTED  
Mean value =  1.2863653000000002  
Радиус = 0.035319891150058534  
Доверительный интервал RC = ( 1.2510454088499416 : 1.3216851911500587 )  
____________________________________  
SELECT REPEATABLE READ  
Mean value =  1.4316854  
Радиус = 0.03150217286157902  
Доверительный интервал RR = ( 1.4001832271384211 : 1.463187572861579 )  
____________________________________  
SELECT SERIALIZE  
Mean value =  2.2273942  
Радиус = 0.2280927603279435  
Доверительный интервал S = ( 1.9993014396720565 : 2.4554869603279434 )  
____________________________________  

Графическое отображение:  

![confidence interval select](Lab4/images/ci_select.png)  

Опять же, доверительные интервалы не перекрываются, значит разница является статистически значимой.  

Доверительные интервалы для запроса **UPDATE**:  
____________________________________  
UPDATE READ COMMITTED  
Mean value =  3.2770301999999996  
Радиус = 0.07581007243802812  
Доверительный интервал RC = ( 3.2012201275619714 : 3.3528402724380277 )  
____________________________________  
UPDATE REPEATABLE READ  
Mean value =  3.6064390000000004  
Радиус = 0.11403020480794468  
Доверительный интервал RR = ( 3.4924087951920555 : 3.7204692048079453 )  
____________________________________  
UPDATE SERIALIZE  
Mean value =  5.740135200000001  
Радиус = 1.206906141432849  
Доверительный интервал S = ( 4.5332290585671515 : 6.9470413414328505 )  
____________________________________  

Графическое отображение:  

![confidence interval update](Lab4/images/ci_update.png)  

И тут тоже доверительные интервалы не перекрываются, значит разница является статистически значимой.  


Попробуем выявить победителей для каждого запроса среди различных уровней изоляции.  
Запрос **INSERT** в среднем быстрее всего выполняется на уровне **REPEATABLE READ**.  
Запрос **SELECT** в среднем быстрее всего выполняется на уровне **READ COMMITTED**.  
Запрос **UPDATE** в среднем быстрее всего выполняется на уровне **READ COMMITTED**.  

Уровень изолированности **SERIALIZABLE** является самым медленным для всех запросов. Это вполне логично, т.к. именно на этом уровне на транзакции накладывается максимальное количество ограничений.


# Вывод #  
В данной лабораторной работе я познакомился с таким понятием как уровни изолированности транзакций. Изучил особенности уровней изолированности в СУБД PostgreSQL.
Экспериментальным путем исследовал все особенности уровней изоляции транзакций к базе данных.
Для этого написал программу на java осуществляющую серии запросов типа INSERT, SELECT, UPDATE при различных уровнях изоляции транзакций.
Далее, полученные метрики отобразил графически и попробовал сравнить производительность транзакций на разных уровнях изоляции.  
По итогу получили следующую картину: быстрее всего запросы в среднем выполняются на уровне изоляции **READ COMMITTED**, а самым "тормозящим" транзакции вполне очевидно оказался уровень **SERIALIZABLE**.
Эти результаты сравнения легко объяснимы: **READ COMMITTED** является самым "свободным" уровнем изоляции, а **SERIALIZABLE** наоборот - самым строгим.

 