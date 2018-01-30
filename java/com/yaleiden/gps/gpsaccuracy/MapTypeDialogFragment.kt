package com.yaleiden.gps.gpsaccuracy

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.yaleiden.gps.gpsaccuracy.R.id.radioButtonSatelite


/**
 * Created by Yale on 1/27/2018.
 */
class MapTypeDialogFragment : DialogFragment() {

    val DEBUG_TAG = "MapTypeDialogFragment"

    val map_types = arrayOf("Normal", "Satelite", "Terrain", "Hybrid")
    var current_selection: Int = 0  // identifies the map type selected


    interface MapDialogListener {
        fun onChangeMapType(value: Int)
    }


    companion object {
        fun newInstance(int: Int): MapTypeDialogFragment {
            val frag = MapTypeDialogFragment()
            val args = Bundle()
            args.putInt("val", int)
            frag.setArguments(args)
            return frag
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.DEBUG) {

            Log.d(DEBUG_TAG, "onCreateView")
        }
        val args = arguments
        current_selection = args.getInt("val")
        val v = inflater?.inflate(R.layout.dialog_map_type, container, false)
        return v
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (BuildConfig.DEBUG) {

            Log.d(DEBUG_TAG, "onViewCreated")
        }

        val radioGroup: RadioGroup = view!!.findViewById(R.id.radioGroup)
        val normal: RadioButton = view.findViewById(R.id.radioButtonNormal)
        val satelite: RadioButton = view.findViewById(R.id.radioButtonSatelite)
        val terrain: RadioButton = view.findViewById(R.id.radioButtonTerrain)
        val hybrid: RadioButton = view.findViewById(R.id.radioButtonHybrid)


        //check the correct button from the argument passed from activity
        if(current_selection == 0 ){
            normal.isChecked = true
        }
        if(current_selection == 1 ){
            satelite.isChecked = true
        }
        if(current_selection == 2 ){
            terrain.isChecked = true
        }
        if(current_selection == 3 ){
            hybrid.isChecked = true
        }

        if (BuildConfig.DEBUG) {

            Log.d(DEBUG_TAG, "onViewCreated 1")
        }
        radioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { radioGroup, i ->

            listener?.onChangeMapType(getRadioButtonInt(radioGroup.checkedRadioButtonId))
            dismiss()
             })
    }

    private var listener: MapDialogListener? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is MapDialogListener) {
            listener = context
        }
    }

    /**
     * @param id_int Int that represents the resource id of the RadioButton selected in dialog
     */
    fun getRadioButtonInt(id_int: Int): Int{
        var butno = 0

        val button: Array<Int> = arrayOf(R.id.radioButtonNormal, R.id.radioButtonSatelite, R.id.radioButtonTerrain, R.id.radioButtonHybrid)

        for(but in button){
            if(button[butno] == id_int){
                return butno
            }
            butno += 1
        }

        return butno
    }
}