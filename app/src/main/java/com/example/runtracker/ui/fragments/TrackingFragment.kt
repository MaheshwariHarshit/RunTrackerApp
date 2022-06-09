package com.example.runtracker.ui.fragments

import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.runtracker.R
import com.example.runtracker.db.Run
import com.example.runtracker.other.Constants.ACTION_PAUSE_SERVICE
import com.example.runtracker.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runtracker.other.Constants.ACTION_STOP_SERVICE
import com.example.runtracker.other.Constants.MAP_ZOOM
import com.example.runtracker.other.Constants.POLYLINE_COLOR
import com.example.runtracker.other.Constants.POLYLINE_WIDTH
import com.example.runtracker.other.TrackingUtility
import com.example.runtracker.services.PolyLine
import com.example.runtracker.services.PolyLines
import com.example.runtracker.services.TrackingService
import com.example.runtracker.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Math.round
import java.util.*
import javax.inject.Inject

const val CANCEL_TRACKING_DIALOG_TAG = "CancelDialog"
@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private val viewModel: MainViewModel by viewModels()
    private var isTracking = false
    private var pathPoints = mutableListOf<PolyLine>()
    private lateinit var mapView: MapView
    private lateinit var btnToggleButton: MaterialButton
    private lateinit var btnFinishRun: MaterialButton
    private lateinit var tvTimer: MaterialTextView
    private var currTimeInMillis = 0L
    private var menu : Menu? = null
    private var map : GoogleMap? = null

    @set: Inject
    var weight = 80f

    override fun onCreateView( //ONLY TO SET HAS MENU = TRUE
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        btnToggleButton = view.findViewById(R.id.btnToggleRun)
        tvTimer = view.findViewById(R.id.tvTimer)
        btnFinishRun = view.findViewById(R.id.btnFinishRun)

        btnToggleButton.setOnClickListener {
            toggleRun()
        }

        if(savedInstanceState != null){
            val cancelTrackingDialogue = parentFragmentManager.findFragmentByTag(
                CANCEL_TRACKING_DIALOG_TAG) as CancelTrackingDialogue?
            cancelTrackingDialogue?.setYesListener {
                stopRun()
            }
        }
        btnFinishRun.setOnClickListener{
            zoomToSeeWholeTrack()
            endRunAndSaveToDb()
        }
        mapView.getMapAsync{
            map = it
            addAllPolyLines()
        }
        subscribeToObservers()
    }

    private fun subscribeToObservers(){
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })
        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyLine()
            moveCameraToUser()
        })

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            currTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(currTimeInMillis,true)
            tvTimer.text = formattedTime
        })
    }

    private fun toggleRun(){ //SETS THE ACTION OF START AND RESUMING THE SERVICE
        if(isTracking){
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(ACTION_PAUSE_SERVICE)
        }
        else{
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) { //TO CREATE THE MENU EXPLICITLY
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu,menu,)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) { // TO MANIPULATE THE AVAILABILITY OF CANCEL MENU
        super.onPrepareOptionsMenu(menu)
        if(currTimeInMillis >0L){
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    private fun stopRun(){
        tvTimer.text = "00:00:00:00"
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.miCancelTracking ->{
                showCancelTrackingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCancelTrackingDialog() {
//        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
//            .setTitle("Cancel Run?")
//            .setMessage("Cancelling the Run will delete the data. Continue?")
//            .setIcon(R.drawable.ic_delete)
//            .setPositiveButton("Yes") { _, _ ->
//                stopRun()
//            }
//            .setNegativeButton("No"){
//                dialogInterface,_->
//                dialogInterface.cancel()
//            }
//            .create()
//        dialog.show()
        CancelTrackingDialogue().apply {
            setYesListener {
                stopRun()
            }
        }.show(parentFragmentManager ,CANCEL_TRACKING_DIALOG_TAG)
    }

    private fun updateTracking(isTracking: Boolean){ //CHECKS THE CURRENT STATE OF TRACKING AND UPDATES THE UI
        this.isTracking = isTracking
        if(!isTracking &&  currTimeInMillis >0L){
            btnToggleButton.text = getString(R.string.Start)
            btnFinishRun.visibility = View.VISIBLE
        }
        else if(isTracking){
            menu?.getItem(0)?.isVisible = true
            btnToggleButton.text = getString(R.string.Stop)
            btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser(){ //THIS FUNCTION KEEPS THE CAMERA TO LAST LOCATION OF THE USER
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()){
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }

    }

    private fun zoomToSeeWholeTrack(){
        val bounds = LatLngBounds.Builder()
        for(polyline in pathPoints){
            for(pos in polyline){
                bounds.include(pos)
            }
        }
        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                mapView.width,
                mapView.height,
                (mapView.height*0.05f).toInt()
            )
        )
    }

    private fun endRunAndSaveToDb() {
        map?.snapshot { bmp ->
            var distanceInMeters = 0
            for(polyline in pathPoints) {
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }
            val avgSpeed = round((distanceInMeters / 1000f) / (currTimeInMillis / 1000f / 60 / 60) * 10) / 10f
            val dateTimestamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()
            val run = Run(bmp, dateTimestamp, avgSpeed, distanceInMeters, currTimeInMillis, caloriesBurned)
            viewModel.insertRun(run)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView),
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
            stopRun()
        }
    }

    private fun addAllPolyLines(){ //WHEN THE DEVICE IS ROTATED WE NEED TO RECREATE ALL THE LINES FOR WHICH THIS FUNCTION IS REQUIRED
        for(polyline in pathPoints){
            val polylineOptions = PolylineOptions()
                .color( POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyLine(){ //THIS IS TO UPDATE THE POLYLINE BY CONNECTING THE LAST 2 POINTS
        if(pathPoints.isNotEmpty() && pathPoints.last().size >1){
            val preLastLatLong = pathPoints.last()[pathPoints.last().size-2]
            val lastLatLong = pathPoints.last().last()
            val polyLineOptions = PolylineOptions()
                .color( POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLong)
                .add(lastLatLong)
            map?.addPolyline(polyLineOptions)
        }
    }

    private fun sendCommandToService(action: String) = Intent(requireContext(),TrackingService::class.java).also {
        it.action = action
        requireContext().startService(it)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
//
    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }
}