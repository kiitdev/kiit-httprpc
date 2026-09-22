package kiit.rpc

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
private data class Person(val name: String, val age: Int)

/** Not kotlinx-backed at all — proves [Serializer] isn't secretly tied to one JSON library. */
private object CsvPersonSerializer : Serializer<Person> {
    override fun encode(value: Person): String = "${value.name},${value.age}"

    override fun decode(content: String): Person {
        val (name, age) = content.split(",")
        return Person(name, age.toInt())
    }
}

class SerializerTest {
    @Test
    fun kotlinxSerializer_round_trips_a_serializable_type() {
        val serializer = KotlinxSerializer(Person.serializer())
        val encoded = serializer.encode(Person("Ada", 30))
        assertEquals(Person("Ada", 30), serializer.decode(encoded))
    }

    @Test
    fun reified_factory_round_trips_the_same_way() {
        val serializer = Serializer<Person>()
        val encoded = serializer.encode(Person("Grace", 40))
        assertEquals(Person("Grace", 40), serializer.decode(encoded))
    }

    @Test
    fun a_hand_written_serializer_satisfies_the_same_contract() {
        val encoded = CsvPersonSerializer.encode(Person("Alan", 25))
        assertEquals("Alan,25", encoded)
        assertEquals(Person("Alan", 25), CsvPersonSerializer.decode(encoded))
    }
}
