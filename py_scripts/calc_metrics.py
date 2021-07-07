import scipy.stats as st
from matplotlib import pyplot as plt
import numpy as np
import json
import os


divider = 1000000
dir = os.path.dirname(__file__)


def mean_confidence_interval(data, confidence=0.95):
    a = 1.0 * np.array(data)
    n = len(a)
    m, se = np.mean(a), st.sem(a)
    h = se * st.t.ppf((1 + confidence) / 2., n-1)
    return m, h, m-h, m+h


def printAverageForEachLevel(query):
    with open(
            os.path.join(dir, '\TransactionsProject\src\main\data\metrics_average.json'),
            "r") \
            as read_total:
        data = json.load(read_total)
        repeatableRead = data['REPEATABLE_READ']
        serializable = data['SERIALIZE']
        readCommitted = data['READ_COMMITTED']
        averageRR = [x / divider for x in repeatableRead[query]]
        averageS = [x / divider for x in serializable[query]]
        averageRC = [x / divider for x in readCommitted[query]]

        x = np.arange(1, 1001, 1)

    print('____________________________________')
    ci_stat = mean_confidence_interval(averageRC)
    print(query + " READ COMMITTED")
    print('Mean value = ', ci_stat[0])
    print('Радиус =', ci_stat[1])
    print('Доверительный интервал RC = (', ci_stat[2], ':', ci_stat[3], ')')
    print('____________________________________')

    ci_stat = mean_confidence_interval(averageRR)
    print(query + " REPEATABLE READ")
    print('Mean value = ', ci_stat[0])
    print('Радиус =', ci_stat[1])
    print('Доверительный интервал RR = (', ci_stat[2], ':', ci_stat[3], ')')
    print('____________________________________')

    ci_stat = mean_confidence_interval(averageS)
    print(query + " SERIALIZE")
    print('Mean value = ', ci_stat[0])
    print('Радиус =', ci_stat[1])
    print('Доверительный интервал S = (', ci_stat[2], ':', ci_stat[3], ')')
    print('____________________________________')

    plt.bar('READ_COMMITTED', mean_confidence_interval(averageRC)[0], yerr=mean_confidence_interval(averageRC)[1], alpha=0.5, ecolor='black', capsize=5)
    plt.bar('REPEATABLE_READ', mean_confidence_interval(averageRR)[0], yerr=mean_confidence_interval(averageRR)[1], alpha=0.5, ecolor='black', capsize=5)
    plt.bar('SERIALIZE', mean_confidence_interval(averageS)[0], yerr=mean_confidence_interval(averageS)[1], alpha=0.5, ecolor='black', capsize=5)
    plt.title(query)
    plt.ylabel("мс")
    plt.show()


def printTotalAllQueryEachTransaction(isolationLevel):
    with open(
            os.path.join(dir, '\TransactionsProject\src\main\data\metrics_average.json'),
            "r") \
            as read_total:
        data = json.load(read_total)
        isolationLevelData = data[isolationLevel]
        insert = [x / divider for x in isolationLevelData['INSERT']]
        select = [x / divider for x in isolationLevelData['SELECT']]
        update = [x / divider for x in isolationLevelData['UPDATE']]

        x = np.arange(1, 1001, 1)


    plt.plot(x, insert, label='INSERT')
    plt.plot(x, select, label='SELECT')
    plt.plot(x, update, label='UPDATE')
    # plt.hlines(insert[999], 1000, 0, label='RC_end', colors='c')
    # plt.hlines(select[999], 1000, 0, label='RR_end', colors='y')
    # plt.hlines(update[999], 1000, 0, label='SER_END', colors='m')
    plt.legend()
    plt.xlabel("номер итерации")
    plt.ylabel("время мс от начала")
    plt.title("TOTAL " + isolationLevel)
    plt.show()





printTotalAllQueryEachTransaction('READ_COMMITTED')
printTotalAllQueryEachTransaction('REPEATABLE_READ')
printTotalAllQueryEachTransaction('SERIALIZE')

printAverageForEachLevel('INSERT')
printAverageForEachLevel('SELECT')
printAverageForEachLevel('UPDATE')