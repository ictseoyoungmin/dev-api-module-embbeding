package com.poc.petgalleryxml.domain

data class Pet(
    val id: String,
    val name: String,
    val emoji: String
)

object MockPets {
    val items: List<Pet> = listOf(
        Pet("pet1", "뽀미", "🐶"),
        Pet("pet2", "초코", "🐱"),
        Pet("pet3", "루이", "🐶")
    )

    fun find(id: String?): Pet? = items.find { it.id == id }
}
