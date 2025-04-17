import kotlin.math.sqrt

fun main () {
    var primeNums = mutableListOf<Int>()

    for (i in 1..50) {
        if (isPrime(i)) {
            primeNums.add(i)
        }
    }

    print("Less or equal 50 primes are : ${primeNums.joinToString()}\n")
    print("Less or equal 50 primes quantity are : ${primeNums.size}\n")

    var twoToTen = mutableListOf<Int>()
    var elevenToTwenty = mutableListOf<Int>()
    var twentyOneToThirty = mutableListOf<Int>()
    var thirtyOneToForty = mutableListOf<Int>()
    var fortyOneToFifty = mutableListOf<Int>()

    primeNums.forEach { num ->
        if (num >= 2 && num <= 10) {
            twoToTen.add(num)
        } else if (num >= 11 && num <= 20) {
            elevenToTwenty.add(num)
        } else if (num >= 21 && num <= 30) {
            twentyOneToThirty.add(num)
        } else if (num >= 31 && num <= 40) {
            thirtyOneToForty.add(num)
        } else if (num >= 41 && num <= 50) {
            fortyOneToFifty.add(num)
        }
    }

    print("2 .. 10 primes are : ${twoToTen.joinToString()}\n")
    print("11 .. 20 primes are : ${elevenToTwenty.joinToString()}\n")
    print("21 .. 30 primes are : ${twentyOneToThirty.joinToString()}\n")
    print("31 .. 40 primes are : ${thirtyOneToForty.joinToString()}\n")
    print("41 .. 50 primes are : ${fortyOneToFifty.joinToString()}")
}

fun isPrime(num: Int): Boolean {
    if (num < 2) return false
    for (i in 2..sqrt(num.toDouble()).toInt()) {
        if (num % i == 0) return false
    }
    return true
}