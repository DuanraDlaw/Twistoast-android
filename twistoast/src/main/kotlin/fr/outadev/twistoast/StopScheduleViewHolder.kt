/*
 * Twistoast - StopScheduleViewHolder.kt
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

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.support.annotation.StringRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import fr.outadev.android.transport.timeo.TimeoStop
import fr.outadev.android.transport.timeo.TimeoStopSchedule
import fr.outadev.android.transport.timeo.TimeoStopTrafficMessage
import fr.outadev.twistoast.uiutils.Colors
import fr.outadev.twistoast.uiutils.collapse
import fr.outadev.twistoast.uiutils.expand
import org.jetbrains.anko.onClick

/**
 * Container for an item in the list. Here, this corresponds to a bus stop, and all the info
 * displayed for it (schedules, and metadata).
 */
class StopScheduleViewHolder(v: View) : RecyclerView.ViewHolder(v) {

    val container: LinearLayout
    val rowLineIdContainer: FrameLayout

    val rowLineId: TextView
    val rowStopName: TextView
    val rowDirectionName: TextView
    val viewScheduleContainer: LinearLayout
    val imgStopWatched: ImageView
    val lineDrawable: GradientDrawable

    val lblStopTrafficTitle: TextView
    val lblStopTrafficMessage: TextView

    val lblScheduleTime = arrayOfNulls<TextView>(RecyclerAdapterRealtime.NB_SCHEDULES_DISPLAYED)
    val lblScheduleDirection = arrayOfNulls<TextView>(RecyclerAdapterRealtime.NB_SCHEDULES_DISPLAYED)
    val lblScheduleSeparator = arrayOfNulls<TextView>(RecyclerAdapterRealtime.NB_SCHEDULES_DISPLAYED)
    val scheduleContainers = arrayOfNulls<View>(RecyclerAdapterRealtime.NB_SCHEDULES_DISPLAYED)

    val viewStopTrafficInfoContainer: View
    val imgStopTrafficExpandIcon: View

    var isExpanded: Boolean

    init {
        val inflater = LayoutInflater.from(v.context)
        container = v as LinearLayout

        isExpanded = false

        // Get references to the views
        rowLineIdContainer = v.findViewById(R.id.rowLineIdContainer) as FrameLayout

        rowLineId = v.findViewById(R.id.rowLineId) as TextView
        rowStopName = v.findViewById(R.id.rowStopName) as TextView
        rowDirectionName = v.findViewById(R.id.rowDirectionName) as TextView

        viewScheduleContainer = v.findViewById(R.id.viewScheduleContainer) as LinearLayout
        imgStopWatched = v.findViewById(R.id.imgStopWatched) as ImageView
        lineDrawable = rowLineIdContainer.background as GradientDrawable

        lblStopTrafficTitle = v.findViewById(R.id.lblStopTrafficTitle) as TextView
        lblStopTrafficMessage = v.findViewById(R.id.lblStopTrafficMessage) as TextView

        viewStopTrafficInfoContainer = v.findViewById(R.id.viewStopTrafficInfoContainer)
        imgStopTrafficExpandIcon = v.findViewById(R.id.imgStopTrafficExpandIcon)

        // Stop traffic info is collapsed by default.
        // When it's clicked, we display the message.
        viewStopTrafficInfoContainer.onClick {
            if (!isExpanded) {
                lblStopTrafficMessage.expand()
                imgStopTrafficExpandIcon
                        .animate()
                        .rotation(180.0f)
                        .start()

            } else {
                lblStopTrafficMessage.collapse()
                imgStopTrafficExpandIcon
                        .animate()
                        .rotation(0f)
                        .start()
            }

            isExpanded = !isExpanded
            viewStopTrafficInfoContainer.requestLayout()
        }

        // Store references to schedule views
        for (i in 0..RecyclerAdapterRealtime.NB_SCHEDULES_DISPLAYED - 1) {
            // Create schedule detail views and make them accessible
            val singleScheduleView = inflater.inflate(R.layout.view_single_schedule_label, null)

            lblScheduleTime[i] = singleScheduleView.findViewById(R.id.lbl_schedule) as TextView
            lblScheduleDirection[i] = singleScheduleView.findViewById(R.id.lbl_schedule_direction) as TextView
            lblScheduleSeparator[i] = singleScheduleView.findViewById(R.id.lbl_schedule_separator) as TextView

            scheduleContainers[i] = singleScheduleView
            viewScheduleContainer.addView(singleScheduleView)
        }
    }

    fun displayStopInfo(stop: TimeoStop) {
        lineDrawable.setColor(Colors.getBrighterColor(Color.parseColor(stop.line.color)))

        rowLineId.text = stop.line.id
        rowStopName.text = rowStopName.context.getString(R.string.stop_name, stop.name)

        val dir = if (stop.line.direction.name != null) stop.line.direction.name else stop.line.direction.id
        rowDirectionName.text = rowDirectionName.context.getString(R.string.direction_name, dir)
    }

    fun displaySchedule(stopSchedule: TimeoStopSchedule) {
        // Get the schedules for this stop
        stopSchedule.schedules.forEachIndexed {
            i, schedule ->
            lblScheduleTime[i]?.text = TimeFormatter.formatTime(lblScheduleTime[i]!!.context, schedule.scheduleTime)
            lblScheduleDirection[i]?.text = schedule.direction
            scheduleContainers[i]?.visibility = View.VISIBLE

            if (!schedule.direction.isNullOrBlank())
                lblScheduleSeparator[i]?.visibility = View.VISIBLE
        }

        if (stopSchedule.schedules.isEmpty()) {
            // If no schedules are available, add a fake one to inform the user
            displayErrorSchedule(R.string.no_upcoming_stops)
        }

        if (stopSchedule.trafficMessages.isNotEmpty()) {
            displayTrafficMessage(stopSchedule.trafficMessages.first())
        }

        startFadeIn()
    }

    fun displayTrafficMessage(message: TimeoStopTrafficMessage) {
        lblStopTrafficTitle.text = message.title
        lblStopTrafficMessage.text = message.body

        viewStopTrafficInfoContainer.visibility = View.VISIBLE
    }

    /**
     * Displays an error message in place of the stop's schedules.
     *
     * @param messageRes the string resource message to display
     * @param invalidStop whether or not to fade out the stop
     */
    fun displayErrorSchedule(@StringRes messageRes: Int, invalidStop: Boolean = false) {
        // Make the row look a bit translucent to make it stand out
        lblScheduleTime[0]?.setText(messageRes)
        scheduleContainers[0]?.visibility = View.VISIBLE

        if (invalidStop)
            container.alpha = 0.4f
    }

    fun startFadeIn() {
        // Fade in the row!
        if (container.alpha != 1.0f) {
            container.alpha = 1.0f

            val alphaAnim = AlphaAnimation(0.4f, 1.0f)
            alphaAnim.duration = 500
            container.startAnimation(alphaAnim)
        }
    }

    /**
     * Reset the row's properties and empty everything.
     */
    fun resetView() {
        // Clear any previous data
        for (i in 0..RecyclerAdapterRealtime.NB_SCHEDULES_DISPLAYED - 1) {
            lblScheduleTime[i]?.text = ""
            lblScheduleDirection[i]?.text = ""
            lblScheduleSeparator[i]?.visibility = View.GONE
            scheduleContainers[i]?.visibility = View.GONE
        }

        viewStopTrafficInfoContainer.visibility = View.GONE
        lblStopTrafficMessage.layoutParams.height = 0
        imgStopTrafficExpandIcon.rotation = 0f

        isExpanded = false
    }

}
