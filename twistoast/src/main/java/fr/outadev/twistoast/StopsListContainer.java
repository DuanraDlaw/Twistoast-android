/*
 * Twistoast - IStopsListContainer
 * Copyright (C) 2013-2014  Baptiste Candellier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.outadev.twistoast;

/**
 * Implements methods a stops list container activity/fragment should have.
 *
 * @author outadoc
 */
public interface StopsListContainer {

	/**
	 * Called when the list of bus stops is refreshed and the user should be notified.
	 */
	public void endRefresh(boolean success);

	/**
	 * Load the fragment at the specified drawer index.
	 *
	 * @param position the index of the element of the drawer we should load
	 */
	public void loadFragmentFromDrawerPosition(int position);

}