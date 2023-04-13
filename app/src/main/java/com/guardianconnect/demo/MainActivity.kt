package com.guardianconnect.demo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.guardianconnect.*
import com.guardianconnect.util.Constants
import com.guardianconnect.util.Constants.Companion.GRD_CONFIG_STRING
import com.guardianconnect.util.GRDKeystore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var etConfig: EditText
    private lateinit var btnStartTunnel: Button
    private lateinit var btnStopTunnel: Button
    private lateinit var btnResetConfiguration: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var etPeToken: EditText
    private lateinit var btnPeToken: Button
    private var adapter: AllRegionsAdapter? = null
    private var rvList: RecyclerView? = null
    private val regionsAdapterList: ArrayList<GRDRegion> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etConfig = findViewById(R.id.etConfig)
        initGRDVPNHelper()
        initUI()
        initRecyclerView()
        loadRegionsList()
        collectFlowStates()

        btnStartTunnel.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            GRDVPNHelper.createAndStartTunnel()
            btnResetConfiguration.isClickable = true
        }

        btnStopTunnel.setOnClickListener {
            GRDVPNHelper.stopTunnel()
            btnStartTunnel.visibility = View.VISIBLE
            btnStopTunnel.visibility = View.GONE
            if (!etConfig.text.isNullOrEmpty()) {
                btnResetConfiguration.isClickable = true
            }
        }

        btnResetConfiguration.setOnClickListener {
            if (!etConfig.text.isNullOrEmpty()) {
                btnResetConfiguration.isClickable = true
            }
            GRDVPNHelper.restartTunnel()
            btnResetConfiguration.isClickable = false
            btnStartTunnel.visibility = View.VISIBLE
            btnStopTunnel.visibility = View.GONE
            etConfig.setText("")
            adapter?.setSelectedPosition(0)
        }

        btnPeToken.setOnClickListener {
            if (!etPeToken.text.isNullOrEmpty()) {
                savePeToken(etPeToken.text.toString())
            } else {
                progressBar.visibility = View.GONE
                Log.d("MainActivity", GRDVPNHelper.GRDVPNHelperStatus.MISSING_PET.name)
            }
        }

        var storedPET = GRDKeystore.instance.retrieveFromKeyStore(Constants.GRD_PE_TOKEN)
        etPeToken.setText(storedPET)
    }

    private fun collectFlowStates() {
        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.configStringFlow.collect {
                etConfig.setText(it)
            }
        }
        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.grdMsgFlow.collect {
                Log.d("MainActivity", it)
                when (it) {
                    GRDTunnelState.SERVER_READY.name -> GRDVPNHelper.prepareVPNPermissions()
                    GRDVPNHelper.GRDVPNHelperStatus.CONNECTED.name -> {
                        progressBar.visibility = View.GONE
                        btnStartTunnel.visibility = View.GONE
                        btnStopTunnel.visibility = View.VISIBLE
                    }
                    else -> {

                    }
                }
            }
        }
        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.grdErrorFlow.collect {
                progressBar.visibility = View.GONE
                Log.e("MainActivity", it)
            }
        }
        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.grdVPNPermissionFlow.collect {
                permissionActivityResultLauncher.launch(it)
            }
        }
    }

    private fun initGRDVPNHelper() {
        GRDVPNHelper.tunnelName = "TUNNEL_NAME"
        GRDVPNHelper.connectAPIHostname = "connect-api.dev.guardianapp.com"
        GRDVPNHelper.setVariables()
        GRDVPNHelper.initHelper(this)
    }

    override fun onPostResume() {
        super.onPostResume()
        val stringFromSharedPrefs = GRDKeystore.instance.retrieveFromKeyStore(GRD_CONFIG_STRING)
        if (!stringFromSharedPrefs.isNullOrEmpty()) etConfig.setText(stringFromSharedPrefs)
    }

    private fun initRecyclerView() {
        adapter = AllRegionsAdapter(regionsAdapterList, onClickListener(), applicationContext)
        rvList?.setHasFixedSize(true)
        rvList?.visibility = View.VISIBLE
        rvList?.layoutManager = LinearLayoutManager(this)
        rvList?.adapter = adapter
        rvList?.addItemDecoration(
            DividerItemDecoration(
                rvList?.context, DividerItemDecoration.VERTICAL
            )
        )
    }

    private fun onClickListener(): IOnClickListener {
        return object : IOnClickListener {
            override fun onClick(grdRegion: GRDRegion?) {
                grdRegion?.let { grdRegion.setPreferredRegion(it) }
            }
        }
    }


    private fun loadRegionsList() {
        GRDVPNHelper.grdServerManager.returnAllAvailableRegions(object :
            GRDServerManager.OnRegionListener {
            override fun onRegionsAvailable(listOfGRDRegions: List<GRDRegion>) {
                regionsAdapterList.addAll(listOfGRDRegions)
                adapter?.notifyDataSetChanged()
                progressBar.visibility = View.GONE
            }
        })
    }

    private fun initUI() {
        btnStartTunnel = findViewById(R.id.btnStartTunnel)
        btnStopTunnel = findViewById(R.id.btnStopTunnel)
        btnResetConfiguration = findViewById(R.id.btnResetConfiguration)
        progressBar = findViewById(R.id.progressBar)
        rvList = findViewById(R.id.rvList)
        etPeToken = findViewById(R.id.etPeToken)
        btnPeToken = findViewById(R.id.btnPeToken)

        if (GRDVPNHelper.isTunnelRunning()) {
            btnStartTunnel.visibility = View.GONE
            btnStopTunnel.visibility = View.VISIBLE
        } else {
            btnStartTunnel.visibility = View.VISIBLE
            btnStopTunnel.visibility = View.GONE
        }
        if (!etConfig.text.isNullOrEmpty()) {
            btnResetConfiguration.isClickable = true
        }
    }

    private fun savePeToken(peToken: String) {
        GRDKeystore.instance.saveToKeyStore(
            Constants.GRD_PE_TOKEN,
            peToken
        )
    }

    private val permissionActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            GRDVPNHelper.startTunnel()
            progressBar.visibility = View.GONE
        }
}