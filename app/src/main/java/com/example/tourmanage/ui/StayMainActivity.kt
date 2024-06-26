package com.example.tourmanage.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.tourmanage.UiState
import com.example.tourmanage.common.ServerGlobal
import com.example.tourmanage.common.data.IntentData
import com.example.tourmanage.common.data.server.item.StayDetailItem
import com.example.tourmanage.common.data.server.item.StayItem
import com.example.tourmanage.common.extension.*
import com.example.tourmanage.common.util.UiController
import com.example.tourmanage.common.value.Config
import com.example.tourmanage.ui.stay.StayMainWidget
import com.example.tourmanage.ui.ui.theme.TourManageTheme
import com.example.tourmanage.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class StayMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent.extras
        val menu = extras?.getString(Config.MAIN_MENU_KEY) ?: "MY APP"

        val viewModel by viewModels<MainViewModel>()
        setContent {
            TourManageTheme {
                StayMainWidget()
//                MainLayout(viewModel, menu)
            }
        }
    }
}

@Composable
fun Header(menuName: String) {
    val context = LocalContext.current
    TopAppBar(
        title = {
            Text(text = menuName)
        },
        navigationIcon = {
            IconButton(onClick = {
                (context as? Activity)?.finish()
            }) {
                Icon(imageVector = Icons.Filled.Home, contentDescription = "Close")
            }
        }
    )
}

@Composable
fun MainLayout(viewModel: MainViewModel = hiltViewModel(), menuName: String) {
    val areaCodes = viewModel.areaInfo.collectAsStateWithLifecycle()
    val stayInfos = viewModel.stayInfo.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.requestAreaList()
        viewModel.requestStayInfo()
    }

    Scaffold(
        topBar = {
            Header(menuName)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(10.dp)) {
            //_ 지역 코드 로드될 때까지 로딩표시
            if (areaCodes.isLoading() || stayInfos.isLoading()) {
                Timber.i("지역 코드 로드중")
                loading()
            } else if (areaCodes.isSuccess()){
                Timber.i("지역 코드 로드 완료")
                AnimatedSearchLayout(viewModel)
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(1.dp))

                if (stayInfos.isSuccess()){
                    val stayItemList = stayInfos.value.data
                    StayListLayout(stayItemList!!, viewModel)
                } else if (stayInfos.isError()){
                    Box(modifier = Modifier
                        .fillMaxHeight(),
                        contentAlignment = Alignment.Center) {
                        Text(text = "검색 결과가 없습니다.",
                            fontSize = 30.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedSearchLayout(viewModel: MainViewModel = hiltViewModel()) {
    var isSearchMode by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    AnimatedVisibility(
        visible = isSearchMode,
        modifier = Modifier
            .fillMaxWidth(),
        enter = scaleIn() + expandHorizontally(),
        exit = scaleOut() + shrinkHorizontally()
    ) {
        BasicTextField(
            modifier = Modifier
                .background(
                    Color(0xDDDDDDDD),
                    RoundedCornerShape(10.dp)
                )
                .padding(4.dp)
                .height(36.dp),
            value = searchText,
            onValueChange = {
                searchText = it
            },
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colors.onSurface,
                fontSize = MaterialTheme.typography.body2.fontSize
            ),
            decorationBox = { innerTextField ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        if (searchText.isEmpty()) {
                            Text(
                                text = "검색어를 입력하세요.",
                                style = LocalTextStyle.current.copy(
                                    color = MaterialTheme.colors.onSurface.copy(alpha =0.3f),
                                    fontSize = MaterialTheme.typography.body2.fontSize
                                    )
                                )
                        }
                        innerTextField()
                    }
                    IconButton(
                        onClick = {
                            viewModel.requestStayInfo(searchText)
                            keyboardController?.hide()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = LocalContentColor.current.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        )
    }
    if (!isSearchMode) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.LightGray, shape = RoundedCornerShape(10.dp))
                .padding(top = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = { isSearchMode = true}
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "",
                    tint = Color.Black
                )

                Text(
                    text = "지역 검색하기",
                    color = Color.Black,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun moveToDetail(viewModel: MainViewModel = hiltViewModel(), contentId: String?) {
    val context = LocalContext.current
    val stayDetailItem = viewModel.stayDetailInfo.collectAsStateWithLifecycle()
    if (contentId != stayDetailItem.value.data?.contentId) {
        Timber.i("stayDetailItem is Not Equal")
        return
    }
    LaunchedEffect(stayDetailItem.value) {
        if (stayDetailItem.isSuccess()) {
            Timber.i("stayDetailItem is Success")
            UiController.addActivity(
                context,
                StayDetailActivity::class,
                IntentData(mapOf(Config.STAY_INFO to stayDetailItem.value.data!!)))
            viewModel._stayDetailInfo.value = UiState.Ready() //FIXME 꼭 이렇게 flow를 초기화 시키는 방법밖에는 없을까 컴포즈에서 제공하는게 있는지 알아 볼 필요 있음.
        } else if (stayDetailItem.isReady()){
            Timber.i("stayDetailItem is READY")
        } else {
            Timber.e("stayDetailItem is failed.")

        }
    }
}
@Composable
fun StayListLayout(stayItemList: ArrayList<StayItem>, viewModel: MainViewModel = hiltViewModel()) {
    val size = stayItemList.size
    Timber.i("StayListLayout() | list size: $size")
    Timber.i("StayListLayout() | stayItemList: $stayItemList")
    LazyColumn() {
        items(
            items = stayItemList,
            itemContent = { StayListItem(it, viewModel) { contentId, contentType ->
                viewModel.requestStayDetailInfo(contentId, contentType)
            } }
        )
    }
}

@Composable
fun StayListItem(stayItem: StayItem, viewModel: MainViewModel = hiltViewModel(), clickEvent: (contentId: String?, contentType: String?) -> Unit) {
    Row(modifier = Modifier
        .padding(5.dp)
        .fillMaxWidth()
        .border(
            border = BorderStroke(width = 1.dp, color = Color.Black),
            shape = RoundedCornerShape(12.5.dp)
        )
        .clickable {
            clickEvent(stayItem.contentId, stayItem.contentTypeId)
        })
    {
        StayImage(stayItem = stayItem)
        Column(modifier = Modifier
            .align(Alignment.CenterVertically)) {
            Text(text = stayItem.title!!.getPureText(), style = MaterialTheme.typography.h6)
            Text(text = stayItem.addr1!!, style = MaterialTheme.typography.caption, modifier = Modifier.padding(top = 5.dp))
        }
    }
    moveToDetail(viewModel, stayItem.contentId)
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun StayImage(stayItem: StayItem) {
    GlideImage(contentScale = ContentScale.Crop,
        model = stayItem.fullImageUrl,
        contentDescription = "",
        modifier = Modifier
            .padding(8.dp)
            .size(84.dp)
            .clip(RoundedCornerShape(CornerSize(16.dp))))
}


@Composable
fun loading() {
    Column {
        repeat(7) {
            LoadingShimmerEffect()
        }
    }
}
@Composable
fun LoadingShimmerEffect() {
    //TODO 코드 분석 필요
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    ShimmerGridItem(brush = brush)
}

@Composable
fun ShimmerGridItem(brush: Brush) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(all = 10.dp), verticalAlignment = Alignment.CenterVertically) {

        Spacer(modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(brush)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Spacer(modifier = Modifier
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .fillMaxWidth(fraction = 0.5f)
                .background(brush)
            )

            Spacer(modifier = Modifier.height(10.dp)) //creates an empty space between
            Spacer(modifier = Modifier
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .fillMaxWidth(fraction = 0.7f)
                .background(brush)
            )
        }
    }
}

fun DismissKeyboard(context: Context) {

}

@Preview(showBackground = true)
@Composable
fun DefaultPreview2() {
    TourManageTheme {
//        RecyclerViewContent()
    }
}