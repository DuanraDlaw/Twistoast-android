/*
 * Twistoast - IStopsListContainer.kt
 * Copyright (C) 2013-2016 Baptiste Candellier
 *
 * Twistoast is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Twistoast is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.outadev.twistoast

/**
 * Implements methods a stops list container activity/fragment should have.
 *
 * @author outadoc
 */
interface IStopsListContainer {

    val isRefreshing: Boolean

    /**
     * Called when the list of bus stops is refreshed and the user should be notified.
     */
    fun endRefresh(success: Boolean)

    fun onUpdatedStopReferences()

    fun setNoContentViewVisible(visible: Boolean)

    /**
     * Load the fragment at the specified drawer index.

     * @param itemId The identifier of the menu item to load
     */
    fun loadFragmentForDrawerItem(itemId: Int)

}
