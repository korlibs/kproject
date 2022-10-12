import com.dragonbones.core.*
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*

suspend fun main() = Korge {
    solidRect(100, 100, Colors.RED).xy(100, 100)
    text(DragonBones.VERSION)
    //println(Adder.add(1, 2).milliseconds)
}
/*
fun main() {
    println(intArrayListOf(1, 2, 3, 4))
    println(10.milliseconds)
}
*/
