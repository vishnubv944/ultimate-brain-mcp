package com.example.model

/** People / CRM database. */
data class PersonModel(
  val id: String,
  val name: String,
  val company: String = "",
  val title: String = "",
  val email: String = "",
  val phone: String = "",
  val relationship: List<String> = emptyList(),
  val pipelineStatus: String? = null,
  val birthday: String? = null,
  val lastCheckIn: String? = null,
  val linkedIn: String = "",
  val twitter: String = "",
  val website: String = "",
  val location: String = "",
)

/** Books database. */
data class BookModel(
  val id: String,
  val title: String,
  val author: String = "",
  val status: String = "Want to Read", // Want to Read | Reading | On Hold | Read
  val rating: String? = null,
  val pages: Int? = null,
  val publishYear: Int? = null,
  val dateStarted: String? = null,
  val dateFinished: String? = null,
  val ownedFormats: List<String> = emptyList(),
  val shelf: List<String> = emptyList(),
  val readNext: Boolean = false,
  val description: String = "",
  val genreIds: List<String> = emptyList(),
)

/** Reading Log entries. */
data class ReadingLogModel(
  val id: String,
  val name: String,
  val bookId: String? = null,
  val bookTitle: String? = null,
  val logDate: String? = null,
  val startPage: Int? = null,
  val endPage: Int? = null,
)

data class GenreModel(val id: String, val name: String, val bookCount: Int = 0)

/** Recipes database. */
data class RecipeModel(
  val id: String,
  val name: String,
  val chef: String = "",
  val prepTime: Int? = null,
  val cookTime: Int? = null,
  val servings: Int? = null,
  val mealTimes: List<String> = emptyList(),
  val favorite: Boolean = false,
  val url: String = "",
)

/** Meal Planner entries. */
data class MealPlanModel(
  val id: String,
  val name: String,
  val date: String? = null,
  val meal: String? = null, // Breakfast | Lunch | Dinner | Snack | Other
  val recipeIds: List<String> = emptyList(),
  val recipeNames: List<String> = emptyList(),
  val favorite: Boolean = false,
)
