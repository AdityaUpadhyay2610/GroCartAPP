
package com.grocart.first.data

import com.grocart.first.R

object DataSource {
    // ✅ DATA SOURCE FOR CATEGORIES
    val categories: List<Categories> = loadCategories()
    fun loadCategories() :List<Categories>{
        return listOf(
            Categories(stringResourceId = R.string.fresh_fruits , imageResourceId = R.drawable.fruits),
            Categories( R.string.bread_biscuits,R.drawable.bread),
            Categories( R.string.sweet_tooth,R.drawable.sweet),
            Categories( R.string.bath_body,R.drawable.bathbody),
            Categories( R.string.beverages,R.drawable.beverages),
            Categories( R.string.kitchen_essentials,R.drawable.kitchen),
            Categories( R.string.munchies,R.drawable.munchies),
            Categories( R.string.packed_food,R.drawable.packaged),
            Categories( R.string.sweet_tooth,R.drawable.sweet),
            Categories( R.string.fresh_vegetables,R.drawable.vegetables),
            Categories( R.string.cleaning_essentials,R.drawable.clean),
            Categories( R.string.stationery,R.drawable.stationary),
            Categories( R.string.pet_food,R.drawable.pet_food)


        )
    }
}
