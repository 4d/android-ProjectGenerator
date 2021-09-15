/*
 * Created by htemanni on 13/9/2021.
 * 4D SAS
 * Copyright (c) 2021 htemanni. All rights reserved.
 */

package models.action


class Action(
        val name: String,
        val icon: String?,
        private val label: String?,
        private val shortLabel: String?,
        val parameters: List<Parameter>
) {

    fun getPreferredName(): String {
        if (!label.isNullOrEmpty())
            return label
        else if (!shortLabel.isNullOrEmpty())
            return shortLabel
        return name
    }
}