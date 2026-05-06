package com.example.openvideo.ui.local

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocalFolderFragment : Fragment() {

    private val viewModel: LocalFolderViewModel by viewModels()
    private lateinit var adapter: VideoFolderAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.loadVideos()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_local_folders, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_folders)
        emptyView = view.findViewById(R.id.tv_empty)

        adapter = VideoFolderAdapter { folder -> openFolder(folder) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<View>(R.id.btn_refresh).setOnClickListener {
            checkPermissionAndLoad()
        }

        observeFolders()
        checkPermissionAndLoad()
    }

    private fun observeFolders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.folders.collect { folders ->
                    adapter.submitList(folders)
                    emptyView.visibility = if (folders.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (folders.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun checkPermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadVideos()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun openFolder(folder: VideoFolder) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                FolderVideosFragment.newInstance(folder.key, folder.name)
            )
            .addToBackStack("folder:${folder.key}")
            .commit()
    }
}
