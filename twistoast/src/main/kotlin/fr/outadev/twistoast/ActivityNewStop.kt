/*
 * Twistoast - ActivityNewStop.kt
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

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import fr.outadev.android.transport.timeo.*
import fr.outadev.twistoast.uiutils.Colors
import kotlinx.android.synthetic.main.activity_new_stop.*
import kotlinx.android.synthetic.main.view_schedule_row.*
import kotlinx.android.synthetic.main.view_single_schedule_label.view.*
import kotlinx.android.synthetic.main.view_toolbar.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread

/**
 * Activity that allows the user to add a bus stop to the app.
 *
 * @author outadoc
 */
class ActivityNewStop : ThemedActivity() {

    private lateinit var itemNext: MenuItem
    private lateinit var databaseHandler: Database
    private var requestHandler: TimeoRequestHandler
    private var lineList: List<TimeoLine>

    init {
        lineList = listOf<TimeoLine>()
        requestHandler = TimeoRequestHandler()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_stop)
        setResult(NO_STOP_ADDED)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white)

        databaseHandler = Database(DatabaseOpenHelper())

        swipeRefreshContainer.setColorSchemeResources(
                R.color.twisto_primary, R.color.twisto_secondary,
                R.color.twisto_primary, R.color.twisto_secondary)

        spinLine.isEnabled = false
        spinDirection.isEnabled = false
        spinStop.isEnabled = false

        setupListeners()
        getLinesFromAPI()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.add_stop, menu)
        itemNext = menu.findItem(R.id.action_ok)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_ok -> {
                // add the current stop to the database
                registerStopToDatabase()
                return true
            }
            android.R.id.home -> {
                // go back
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Initialises, sets up the even listeners, and populates the line spinner.
     */
    private fun setupListeners() {
        // when a line has been selected
        spinLine.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parentView: AdapterView<*>, view: View, position: Int, id: Long) {
                // set loading labels
                rowLineId.text = ""
                rowDirectionName.text = resources.getString(R.string.loading_data)
                rowStopName.text = resources.getString(R.string.loading_data)

                viewScheduleContainer.removeAllViewsInLayout()
                viewScheduleContainer.visibility = View.GONE

                spinStop.isEnabled = false
                itemNext.isEnabled = false

                // get the selected line
                val item = currentLine

                if (item != null) {
                    // set the line view
                    rowLineId.text = item.id

                    val lineDrawable = rowLineIdContainer.background as GradientDrawable
                    lineDrawable.setColor(Colors.getBrighterColor(Color.parseColor(item.color)))

                    spinDirection.isEnabled = true

                    // adapt the size based on the size of the line ID
                    if (rowLineId.text.length > 3) {
                        rowLineId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    } else if (rowLineId.text.length > 2) {
                        rowLineId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f)
                    } else {
                        rowLineId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
                    }

                    spinDirection.adapter = ArrayAdapter(this@ActivityNewStop, android.R.layout.simple_spinner_dropdown_item, getDirectionsList())
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
            }

        }

        // when a direction has been selected
        spinDirection.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parentView: AdapterView<*>, view: View, position: Int, id: Long) {
                // set loading labels
                rowDirectionName.text = resources.getString(R.string.loading_data)
                viewScheduleContainer.removeAllViewsInLayout()
                viewScheduleContainer.visibility = View.GONE

                itemNext.isEnabled = false
                spinStop.isEnabled = false

                if (currentLine != null
                        && currentDirection != null
                        && currentLine != null
                        && currentDirection != null) {

                    swipeRefreshContainer.isEnabled = true
                    swipeRefreshContainer.isRefreshing = true

                    val dir = if (currentDirection!!.name != null) currentDirection!!.name else currentDirection!!.id
                    rowDirectionName.text = resources.getString(R.string.direction_name, dir)

                    currentLine!!.direction = currentDirection!!

                    doAsync {
                        try {
                            val timeoStops = requestHandler.getStops(currentLine!!)

                            uiThread {
                                spinStop.isEnabled = true
                                spinStop.adapter = ArrayAdapter(this@ActivityNewStop, android.R.layout.simple_spinner_dropdown_item, timeoStops)
                            }
                        } catch (e: Exception) {
                            uiThread { handleAsyncExceptions(e) }
                        }
                    }
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
            }
        }

        // when a stop has been selected
        spinStop.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parentView: AdapterView<*>, view: View, position: Int, id: Long) {
                rowStopName.text = resources.getString(R.string.loading_data)
                viewScheduleContainer.removeAllViewsInLayout()
                viewScheduleContainer.visibility = View.GONE

                val stop = currentStop
                itemNext.isEnabled = true

                if (stop != null && true) {
                    rowStopName.text = resources.getString(R.string.stop_name, stop.name)
                    updateSchedulePreview()
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
            }
        }
    }

    private fun updateSchedulePreview() {
        swipeRefreshContainer.isEnabled = true
        swipeRefreshContainer.isRefreshing = true

        doAsync {
            try {
                val schedule = requestHandler.getSingleSchedule(currentStop!!)

                uiThread {
                    val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                    swipeRefreshContainer.isEnabled = false
                    swipeRefreshContainer.isRefreshing = false

                    val schedList = schedule.schedules

                    // set the schedule labels, if we need to
                    for (currSched in schedList) {
                        val singleScheduleView = inflater.inflate(R.layout.view_single_schedule_label, null)

                        singleScheduleView.lbl_schedule.text = TimeFormatter.formatTime(this@ActivityNewStop, currSched.scheduleTime)
                        singleScheduleView.lbl_schedule_direction.text = currSched.direction

                        if (!currSched.direction.isNullOrBlank())
                            singleScheduleView.lbl_schedule_separator.visibility = View.VISIBLE

                        viewScheduleContainer.addView(singleScheduleView)
                    }

                    if (schedList.isNotEmpty()) {
                        viewScheduleContainer.visibility = View.VISIBLE
                    }
                }

            } catch (e: Exception) {
                uiThread { handleAsyncExceptions(e) }
            }
        }
    }

    /**
     * Fetches the bus lines from the API, and populates the line spinner when done.
     */
    private fun getLinesFromAPI() {
        swipeRefreshContainer.isEnabled = true
        swipeRefreshContainer.isRefreshing = true

        doAsync {
            try {
                val timeoLines = requestHandler.getLines()

                uiThread {
                    lineList = timeoLines
                    spinLine.adapter = ArrayAdapter(this@ActivityNewStop, android.R.layout.simple_spinner_dropdown_item, timeoLines.distinctBy(TimeoLine::id))

                    spinLine.isEnabled = true
                    spinDirection.isEnabled = true
                }
            } catch (e: Exception) {
                uiThread { handleAsyncExceptions(e) }
            }

        }
    }

    /**
     * Displays an exception in a toast on the UI thread.

     * @param e the exception to display
     */
    private fun handleAsyncExceptions(e: Exception) {
        e.printStackTrace()

        if (e is TimeoBlockingMessageException) {
            e.getAlertMessage(this@ActivityNewStop).show()
            return
        }

        val message: String

        if (e is TimeoException) {
            if (!e.message?.trim { it <= ' ' }!!.isEmpty()) {
                message = getString(R.string.error_toast_twisto_detailed, e.errorCode, e.message)
            } else {
                message = getString(R.string.error_toast_twisto, e.errorCode)
            }

        } else {
            message = getString(R.string.loading_error)
        }

        swipeRefreshContainer.isEnabled = false
        swipeRefreshContainer.isRefreshing = false

        Snackbar.make(findViewById(R.id.content), message, Snackbar.LENGTH_LONG).setAction(R.string.error_retry) { getLinesFromAPI() }.show()
    }

    /**
     * Stores the selected bus stop in the database.
     */
    private fun registerStopToDatabase() {
        try {
            databaseHandler.addStopToDatabase(currentStop)
            toast(resources.getString(R.string.added_toast, currentStop.toString()))
            setResult(STOP_ADDED)
            finish()
        } catch (e: SQLiteConstraintException) {
            // stop already in database
            longToast(resources.getString(R.string.error_toast, resources.getString(R.string.add_error_duplicate)))
        } catch (e: IllegalArgumentException) {
            // one of the fields was null
            longToast(resources.getString(R.string.error_toast, resources.getString(R.string.add_error_illegal_argument)))
        }
    }

    /**
     * Gets a list of directions for the selected bus line, as they're stored in the same object.

     * @return a list of ID/name objects containing the id and name of the directions to display
     */
    private fun getDirectionsList(): List<TimeoDirection> {
        return lineList.filter { line -> line.id == currentLine?.id }.map(TimeoLine::direction)
    }

    /**
     * Gets the bus stop that's currently selected.

     * @return a stop
     */
    val currentStop: TimeoStop?
        get() = spinStop.getItemAtPosition(spinStop.selectedItemPosition) as TimeoStop

    /**
     * Gets the bus line direction that's currently selected.

     * @return an ID/name object for the direction
     */
    val currentDirection: TimeoDirection?
        get() = spinDirection.getItemAtPosition(spinDirection.selectedItemPosition) as TimeoDirection

    /**
     * Gets the bus line that's currently selected.

     * @return a line
     */
    var currentLine: TimeoLine? = null
        get() = spinLine.getItemAtPosition(spinLine.selectedItemPosition) as TimeoLine

    companion object {
        const val NO_STOP_ADDED = 0
        const val STOP_ADDED = 1
    }

}
