package top.niunaijun.blackboxa.view.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.roger.catloadinglibrary.CatLoadingView
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.databinding.FragmentAppsBinding
import top.niunaijun.blackboxa.util.InjectionUtil
import top.niunaijun.blackboxa.util.LoadingUtil
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.main.MainActivity

/**
 *
 * @Description:
 * @Author: wukaicheng
 * @CreateDate: 2021/4/29 22:21
 */
class AppsFragment(private val userID: Int) : Fragment() {

    private lateinit var viewModel: AppsViewModel

    private lateinit var mAdapter: AppsAdapter

    private val viewBinding: FragmentAppsBinding by inflate()

    private lateinit var loadingView: CatLoadingView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, InjectionUtil.getAppsFactory()).get(AppsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mAdapter = AppsAdapter()
        viewBinding.recyclerView.adapter = mAdapter
        viewBinding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        viewBinding.stateView.showEmpty()

        mAdapter.setOnItemClick { _, _, data ->
            showLoading()
            viewModel.launchApk(data.packageName, userID)
        }

        mAdapter.setOnItemLongClick { _, _, data ->
            unInstallApk(data.packageName)
        }
        return viewBinding.root
    }

    override fun onStart() {
        super.onStart()
        viewBinding.stateView.showLoading()
        viewModel.getInstalledApps(userID)
        viewModel.appsLiveData.observe(this) {
            if (it != null) {
                mAdapter.replaceData(it)
                if (it.isEmpty()) {
                    viewBinding.stateView.showEmpty()
                } else {
                    viewBinding.stateView.showContent()
                }
            } else {
                viewBinding.stateView.showEmpty()
            }
        }

        viewModel.resultLiveData.observe(this) {
            it?.run {
                hideLoading()
                if (it) {
                    requireContext().toast("操作成功")
                    viewModel.getInstalledApps(userID)
                    scanUser()
                } else {
                    requireContext().toast("操作失败")
                }
            }

        }

        viewModel.launchLiveData.observe(this) {
            it?.run {
                hideLoading()
                if (!it) {
                    Toast.makeText(requireContext(), "启动失败", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.appsLiveData.value = null
        viewModel.appsLiveData.removeObservers(this)
        viewModel.resultLiveData.value = null
        viewModel.resultLiveData.removeObservers(this)
        viewModel.launchLiveData.value = null
        viewModel.launchLiveData.removeObservers(this)
    }


    private fun unInstallApk(packageName: String) {
        MaterialDialog(requireContext()).show {
            title(text = "卸载软件")
            message(text = "是否卸载该软件，卸载后相关数据将被清除？")
            positiveButton(text = "确定") {
                showLoading()
                viewModel.unInstall(packageName, userID)
            }
            negativeButton(text = "取消")
        }
    }

    fun installApk(source: String) {
        val packageName = if (URLUtil.isValidUrl(source)) {
            val info = requireContext().packageManager.getPackageArchiveInfo(source, 0)
            info.packageName

        } else {
            source
        }

        if (BlackBoxCore.get().isInstalled(packageName, userID)) {
            MaterialDialog(requireContext()).show {
                title(text = "覆盖安装")
                message(text = "该软件已经安装到此用户，是否覆盖安装？")
                positiveButton(text = "覆盖安装") {
                    showLoading()
                    viewModel.install(source, userID)
                }
                negativeButton(text = "取消安装")
            }
        } else {
            showLoading()
            viewModel.install(source, userID)
        }

    }


    private fun scanUser() {
        (requireActivity() as MainActivity).scanUser()
    }

    private fun showLoading() {
        if (!this::loadingView.isInitialized) {
            loadingView = CatLoadingView()
        }

        LoadingUtil.showLoading(loadingView, childFragmentManager)
    }


    private fun hideLoading() {
        if (this::loadingView.isInitialized) {
            loadingView.dismiss()
        }
    }

}