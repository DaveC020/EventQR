package com.thedavelopers.eventqr.features.attendee

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.core.util.RoleMapper
import com.thedavelopers.eventqr.features.users.model.dto.UserResponse
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

private const val TAG = "AttendeeProfile"

open class AttendeeProfileActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: AttendeeRepository
    private lateinit var txtProfileName: TextView
    private lateinit var txtProfileRole: TextView
    private lateinit var txtProfileEmail: TextView
    private lateinit var txtPhone: TextView
    private lateinit var imgProfileAvatar: ImageView
    private lateinit var imgProfileAvatarPlaceholder: View
    private lateinit var progressProfileLoading: ProgressBar
    private lateinit var txtProfileError: TextView
    private lateinit var btnProfileRetry: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)
        repository = AttendeeRepository(this)

        txtProfileName = findViewById(R.id.txtProfileName)
        txtProfileRole = findViewById(R.id.txtProfileRole)
        txtProfileEmail = findViewById(R.id.txtProfileEmail)
        txtPhone = findViewById(R.id.txtPhone)
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar)
        imgProfileAvatarPlaceholder = findViewById(R.id.imgProfileAvatarPlaceholder)
        progressProfileLoading = findViewById(R.id.progressProfileLoading)
        txtProfileError = findViewById(R.id.txtProfileError)
        btnProfileRetry = findViewById(R.id.btnProfileRetry)

        btnProfileRetry.setOnClickListener { loadProfile() }

        findViewById<View>(R.id.cardEditProfile).setOnClickListener {
            startActivity(Intent(this, AttendeeEditProfileActivity::class.java))
        }
        findViewById<View>(R.id.cardMyEvents).setOnClickListener {
            startActivity(Intent(this, RegisteredEventsActivity::class.java))
        }
        findViewById<View>(R.id.cardTransactionHistory).setOnClickListener {
            startActivity(Intent(this, AttendeeTransactionsActivity::class.java))
        }
        findViewById<View>(R.id.cardClaimedRewards).setOnClickListener {
            startActivity(Intent(this, ClaimedRewardsActivity::class.java))
        }
        findViewById<View>(R.id.cardMyEventRequests).setOnClickListener {
            startActivity(Intent(this, MyEventRequestsActivity::class.java))
        }
        findViewById<View>(R.id.cardNotifications).setOnClickListener {
            startActivity(Intent(this, AttendeeNotificationsActivity::class.java))
        }

        findViewById<Button>(R.id.btnProfileLogout).setOnClickListener {
            clearAvatarCache(filesDir)
            sessionManager.clearSession()
            startActivity(
                Intent(this, com.thedavelopers.eventqr.features.auth.login.LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }

        configureAttendeeBottomNav(AttendeeBottomNavItem.PROFILE)
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        setLoadingState(true)
        clearErrorState()

        renderProfile(null)
        renderAvatar(imgProfileAvatar, imgProfileAvatarPlaceholder, loadBitmapFromLocalPath(sessionManager.getAvatarLocalPath()))

        lifecycleScope.launch {
            when (val result = repository.getMyProfile()) {
                is NetworkResult.Success -> {
                    val user = result.data
                    sessionManager.updateProfile(user.fullName, user.phoneNumber, user.email)
                    sessionManager.saveRole(user.role)
                    sessionManager.saveAvatarFileId(user.avatarFileId)
                    renderProfile(user)

                    val avatar = loadAvatarBitmapWithCache(
                        repository = repository,
                        filesDir = filesDir,
                        userId = user.userId.toString(),
                        avatarPath = user.avatarPath,
                        avatarFileId = user.avatarFileId,
                    ) ?: CachedAvatar(loadBitmapFromLocalPath(sessionManager.getAvatarLocalPath()), null)

                    renderAvatar(imgProfileAvatar, imgProfileAvatarPlaceholder, avatar.bitmap)
                    avatar.cachedFile?.let { sessionManager.setAvatarLocalPath(it.absolutePath) }
                    clearErrorState()
                }
                is NetworkResult.Error -> showErrorState(result.message.ifBlank { "Unable to load profile." })
                else -> Unit
            }

            setLoadingState(false)
        }
    }

    private fun renderProfile(user: UserResponse? = null) {
        txtProfileName.text = user?.fullName ?: sessionManager.getFullName().orEmpty()
        txtProfileRole.text = (user?.role?.name ?: sessionManager.getUserRole())
            ?.takeIf { it.isNotBlank() }
            ?.let { RoleMapper.getDisplayName(it) }
            .orEmpty()
        txtProfileEmail.text = user?.email ?: sessionManager.getEmail().orEmpty()
        txtPhone.text = user?.phoneNumber ?: sessionManager.getPhone().orEmpty()
    }

    private fun setLoadingState(loading: Boolean) {
        progressProfileLoading.visibility = if (loading) View.VISIBLE else View.GONE
        btnProfileRetry.visibility = View.GONE
        txtProfileError.visibility = View.GONE
    }

    private fun showErrorState(message: String) {
        txtProfileError.text = message
        txtProfileError.visibility = View.VISIBLE
        btnProfileRetry.visibility = View.VISIBLE
    }

    private fun clearErrorState() {
        txtProfileError.visibility = View.GONE
        btnProfileRetry.visibility = View.GONE
    }
}

open class AttendeeEditProfileActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: AttendeeRepository

    private lateinit var btnBack: ImageButton
    private lateinit var edtFullName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPhone: EditText
    private lateinit var imgAvatar: ImageView
    private lateinit var imgAvatarPlaceholder: ImageView
    private lateinit var txtChangePhoto: TextView
    private lateinit var txtApiError: TextView
    private lateinit var btnRetryProfileLoad: Button
    private lateinit var progressLoading: ProgressBar
    private lateinit var btnSaveChanges: Button

    private var initialFullName: String = ""
    private var initialEmail: String = ""
    private var initialPhone: String = ""
    private var initialAvatarFileId: String? = null

    private var selectedAvatarFile: File? = null
    private var avatarChanged: Boolean = false
    private var isLoadingProfile: Boolean = false
    private var isSavingProfile: Boolean = false

    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult

        clearApiError()
        val avatarFile = cacheSelectedAvatar(uri)
        if (avatarFile == null) {
            showApiError("Unable to read selected photo. Please try a different image.")
            return@registerForActivityResult
        }

        selectedAvatarFile = avatarFile
        avatarChanged = true
        renderAvatar(imgAvatar, imgAvatarPlaceholder, loadBitmapFromFile(avatarFile))
        updateSaveButtonState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)
        repository = AttendeeRepository(this)

        bindViews()
        bindActions()
        prefillFromSession()
        loadCurrentProfile()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBackImage)
        edtFullName = findViewById(R.id.edtFullName)
        edtEmail = findViewById(R.id.edtEmail)
        edtPhone = findViewById(R.id.edtPhone)
        imgAvatar = findViewById(R.id.imgAvatar)
        imgAvatarPlaceholder = findViewById(R.id.imgAvatarPlaceholder)
        txtChangePhoto = findViewById(R.id.txtChangePhoto)
        txtApiError = findViewById(R.id.txtApiError)
        btnRetryProfileLoad = findViewById(R.id.btnRetryProfileLoad)
        progressLoading = findViewById(R.id.progressLoading)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
    }

    private fun bindActions() {
        btnBack.setOnClickListener { finish() }
        btnRetryProfileLoad.setOnClickListener { loadCurrentProfile() }
        txtChangePhoto.setOnClickListener {
            if (isLoadingProfile || isSavingProfile) return@setOnClickListener
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnSaveChanges.setOnClickListener { attemptSave() }

        val formWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                clearApiError()
                clearFieldErrors()
                updateSaveButtonState()
            }
        }

        edtFullName.addTextChangedListener(formWatcher)
        edtEmail.addTextChangedListener(formWatcher)
        edtPhone.addTextChangedListener(formWatcher)
    }

    private fun prefillFromSession() {
        edtFullName.setText(sessionManager.getFullName().orEmpty())
        edtEmail.setText(sessionManager.getEmail().orEmpty())
        edtPhone.setText(sessionManager.getPhone().orEmpty())

        initialAvatarFileId = sessionManager.getAvatarFileId()
        renderAvatar(imgAvatar, imgAvatarPlaceholder, loadBitmapFromLocalPath(sessionManager.getAvatarLocalPath()))

        captureInitialFormSnapshot()
        updateSaveButtonState()
    }

    private fun loadCurrentProfile() {
        setLoadingState(true)
        lifecycleScope.launch {
            when (val profileResult = repository.getMyProfile()) {
                is NetworkResult.Success -> {
                    val user = profileResult.data
                    edtFullName.setText(user.fullName)
                    edtEmail.setText(user.email)
                    edtPhone.setText(user.phoneNumber.orEmpty())

                    sessionManager.updateProfile(
                        fullName = user.fullName,
                        phone = user.phoneNumber,
                        email = user.email
                    )
                    sessionManager.saveRole(user.role)
                    sessionManager.saveAvatarFileId(user.avatarFileId)
                    initialAvatarFileId = user.avatarFileId

                    val avatar = loadAvatarBitmapWithCache(
                        repository = repository,
                        filesDir = filesDir,
                        userId = user.userId.toString(),
                        avatarPath = user.avatarPath,
                        avatarFileId = user.avatarFileId,
                    ) ?: CachedAvatar(loadBitmapFromLocalPath(sessionManager.getAvatarLocalPath()), null)

                    renderAvatar(imgAvatar, imgAvatarPlaceholder, avatar.bitmap)
                    avatar.cachedFile?.let { sessionManager.setAvatarLocalPath(it.absolutePath) }
                }

                is NetworkResult.Error -> showApiError(profileResult.message)
                else -> Unit
            }

            captureInitialFormSnapshot()
            setLoadingState(false)
        }
    }

    private fun attemptSave() {
        clearApiError()
        clearFieldErrors()

        if (!validateForm()) return

        if (sanitizeEmail() != initialEmail) {
            edtEmail.error = "Email updates are not supported."
            showApiError("Email updates are not supported by this account endpoint.")
            return
        }

        if (!hasChanges()) return

        isSavingProfile = true
        updateSaveButtonState()

        val fullName = sanitizeName()
        val phone = sanitizePhone().ifBlank { null }

        lifecycleScope.launch {
            when (val updateResult = repository.updateProfile(fullName, phone)) {
                is NetworkResult.Success -> {
                    if (avatarChanged) {
                        val avatarFile = selectedAvatarFile
                        if (avatarFile == null || !avatarFile.exists()) {
                            showApiError("Selected photo is unavailable. Please choose the image again.")
                            isSavingProfile = false
                            updateSaveButtonState()
                            return@launch
                        }
                        when (val avatarResult = repository.uploadAvatar(avatarFile)) {
                            is NetworkResult.Error -> {
                                showApiError(avatarResult.message)
                                isSavingProfile = false
                                updateSaveButtonState()
                                return@launch
                            }

                            is NetworkResult.Success -> {
                                val uploadedAvatar = avatarResult.data
                                val uploadedAvatarFileId = uploadedAvatar.fileId.toString()
                                val cachedFile = copyAvatarFileToCache(
                                    filesDir = filesDir,
                                    userId = sessionManager.getUserId(),
                                    avatarFileId = uploadedAvatarFileId,
                                    sourceFile = avatarFile,
                                ) ?: avatarFile

                                selectedAvatarFile = cachedFile
                                sessionManager.saveAvatarFileId(uploadedAvatarFileId)
                                sessionManager.setAvatarLocalPath(cachedFile.absolutePath)
                                initialAvatarFileId = uploadedAvatarFileId
                                avatarChanged = false
                                renderAvatar(imgAvatar, imgAvatarPlaceholder, loadBitmapFromFile(cachedFile))
                            }

                            else -> Unit
                        }
                    }

                    refreshProfileStateFromBackend()

                    sessionManager.updateProfile(fullName, phone, initialEmail)
                    sessionManager.saveAvatarFileId(initialAvatarFileId)
                    selectedAvatarFile?.takeIf { it.exists() }?.let { sessionManager.setAvatarLocalPath(it.absolutePath) }
                    captureInitialFormSnapshot()
                    Toast.makeText(this@AttendeeEditProfileActivity, "Profile updated successfully.", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }

                is NetworkResult.Error -> showApiError(updateResult.message)
                else -> Unit
            }

            isSavingProfile = false
            updateSaveButtonState()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (sanitizeName().isBlank()) {
            edtFullName.error = "Full name is required."
            isValid = false
        }

        val email = sanitizeEmail()
        if (email.isBlank()) {
            edtEmail.error = "Email address is required."
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.error = "Enter a valid email address."
            isValid = false
        }

        val phone = sanitizePhone()
        if (phone.isBlank()) {
            edtPhone.error = "Phone number is required."
            isValid = false
        } else if (!isPhoneValid(phone)) {
            edtPhone.error = "Enter a valid phone number."
            isValid = false
        }

        return isValid
    }

    private fun isPhoneValid(phone: String): Boolean {
        val pattern = Regex("^[+]?[-0-9()\\s]{7,20}$")
        val digits = phone.count { it.isDigit() }
        return pattern.matches(phone) && digits in 7..15
    }

    private fun hasChanges(): Boolean {
        return sanitizeName() != initialFullName ||
            sanitizeEmail() != initialEmail ||
            sanitizePhone() != initialPhone ||
            avatarChanged
    }

    private fun captureInitialFormSnapshot() {
        initialFullName = sanitizeName()
        initialEmail = sanitizeEmail()
        initialPhone = sanitizePhone()
    }

    private fun sanitizeName(): String = edtFullName.text.toString().trim()

    private fun sanitizeEmail(): String = edtEmail.text.toString().trim()

    private fun sanitizePhone(): String = edtPhone.text.toString().trim()

    private fun clearFieldErrors() {
        edtFullName.error = null
        edtEmail.error = null
        edtPhone.error = null
    }

    private fun showApiError(message: String) {
        txtApiError.text = message
        txtApiError.visibility = View.VISIBLE
        btnRetryProfileLoad.visibility = View.VISIBLE
    }

    private fun clearApiError() {
        txtApiError.text = ""
        txtApiError.visibility = View.GONE
        btnRetryProfileLoad.visibility = View.GONE
    }

    private fun setLoadingState(loading: Boolean) {
        isLoadingProfile = loading
        progressLoading.visibility = if (loading) View.VISIBLE else View.GONE

        edtFullName.isEnabled = !loading
        edtEmail.isEnabled = !loading
        edtPhone.isEnabled = !loading
        txtChangePhoto.isEnabled = !loading

        updateSaveButtonState()
    }

    private fun updateSaveButtonState() {
        val canSave = !isLoadingProfile && !isSavingProfile && hasChanges()
        btnSaveChanges.isEnabled = canSave
        btnSaveChanges.text = if (isSavingProfile) "Saving..." else "Save Changes"
    }

    private fun cacheSelectedAvatar(uri: Uri): File? {
        return runCatching {
            val avatarDirectory = avatarCacheDirectory(filesDir)
            val userKey = sessionManager.getUserId()?.takeIf { it.isNotBlank() } ?: "current"
            val targetFile = File(avatarDirectory, "avatar_${userKey}_selected.jpg")

            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return null
                FileOutputStream(targetFile).use { output -> input.copyTo(output) }
            }
            targetFile
        }.getOrNull()
    }

    private suspend fun refreshProfileStateFromBackend() {
        when (val result = repository.getMyProfile()) {
            is NetworkResult.Success -> {
                val user = result.data
                sessionManager.updateProfile(user.fullName, user.phoneNumber, user.email)
                sessionManager.saveRole(user.role)
                sessionManager.saveAvatarFileId(user.avatarFileId)
                initialAvatarFileId = user.avatarFileId

                val avatar = loadAvatarBitmapWithCache(
                    repository = repository,
                    filesDir = filesDir,
                    userId = user.userId.toString(),
                    avatarPath = user.avatarPath,
                    avatarFileId = user.avatarFileId,
                ) ?: selectedAvatarFile?.let { CachedAvatar(loadBitmapFromFile(it), it) }
                    ?: CachedAvatar(loadBitmapFromLocalPath(sessionManager.getAvatarLocalPath()), null)

                renderAvatar(imgAvatar, imgAvatarPlaceholder, avatar.bitmap)
                avatar.cachedFile?.let { sessionManager.setAvatarLocalPath(it.absolutePath) }
                if (avatar.bitmap == null && !user.avatarFileId.isNullOrBlank()) {
                    Log.w(TAG, "Profile refresh succeeded, but avatar preview data was not available.")
                }
            }

            is NetworkResult.Error -> Log.w(TAG, "Profile refresh after save failed: ${result.message}")
            else -> Unit
        }
    }
}

private data class CachedAvatar(
    val bitmap: Bitmap?,
    val cachedFile: File?,
)

private fun resolveAvatarPath(avatarPath: String?, avatarFileId: String?): String? {
    return avatarPath?.takeIf { it.isNotBlank() }
        ?: avatarFileId?.takeIf { it.isNotBlank() }?.let { "files/$it/content" }
}

private fun avatarCacheDirectory(filesDir: File): File {
    return File(filesDir, "avatars").apply {
        if (!exists()) mkdirs()
    }
}

private fun avatarCacheKey(avatarPath: String?, avatarFileId: String?): String? {
    return avatarFileId?.takeIf { it.isNotBlank() }
        ?: avatarPath?.takeIf { it.isNotBlank() }?.hashCode()?.toString()
}

private fun resolveAvatarCacheFile(filesDir: File, userId: String?, avatarPath: String?, avatarFileId: String?): File? {
    val cleanUserId = userId?.takeIf { it.isNotBlank() } ?: return null
    val cleanKey = avatarCacheKey(avatarPath, avatarFileId) ?: return null
    return File(avatarCacheDirectory(filesDir), "avatar_${cleanUserId}_$cleanKey.jpg")
}

private suspend fun loadAvatarBitmapWithCache(
    repository: AttendeeRepository,
    filesDir: File,
    userId: String?,
    avatarPath: String?,
    avatarFileId: String?,
): CachedAvatar? {
    val remotePath = resolveAvatarPath(avatarPath, avatarFileId) ?: return null
    val cacheFile = resolveAvatarCacheFile(filesDir, userId, remotePath, avatarFileId)

    if (cacheFile?.exists() == true) {
        loadBitmapFromFile(cacheFile)?.let { return CachedAvatar(it, cacheFile) }
    }

    return when (val result = repository.downloadAvatar(remotePath)) {
        is NetworkResult.Success -> {
            val bytes = result.data
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val savedFile = cacheFile?.let { file ->
                runCatching {
                    FileOutputStream(file).use { output -> output.write(bytes) }
                    file
                }.onFailure { Log.w(TAG, "Unable to cache avatar image.", it) }.getOrNull()
            }
            CachedAvatar(bitmap, savedFile)
        }
        else -> null
    }
}

private fun loadBitmapFromLocalPath(localPath: String?): Bitmap? {
    val file = localPath
        ?.takeIf { it.isNotBlank() }
        ?.let { File(it) }
        ?.takeIf { it.exists() && it.isFile }
    return file?.let { loadBitmapFromFile(it) }
}

private fun loadBitmapFromFile(file: File): Bitmap? {
    return runCatching { BitmapFactory.decodeFile(file.absolutePath) }
        .onFailure { Log.w(TAG, "Unable to decode avatar file.", it) }
        .getOrNull()
}

private fun copyAvatarFileToCache(filesDir: File, userId: String?, avatarFileId: String?, sourceFile: File): File? {
    val targetFile = resolveAvatarCacheFile(filesDir, userId, "files/$avatarFileId/content", avatarFileId) ?: return null
    return runCatching {
        sourceFile.copyTo(targetFile, overwrite = true)
        targetFile
    }.onFailure { Log.w(TAG, "Unable to copy selected avatar into cache.", it) }.getOrNull()
}

private fun clearAvatarCache(filesDir: File) {
    runCatching {
        avatarCacheDirectory(filesDir).deleteRecursively()
    }.onFailure { Log.w(TAG, "Unable to clear avatar cache.", it) }
}

private fun renderAvatar(imageView: ImageView, placeholder: View, bitmap: Bitmap?) {
    if (bitmap == null) {
        imageView.setImageDrawable(null)
        imageView.visibility = View.GONE
        placeholder.visibility = View.VISIBLE
        return
    }

    imageView.setImageBitmap(toCircularBitmap(bitmap))
    imageView.visibility = View.VISIBLE
    placeholder.visibility = View.GONE
}

private fun toCircularBitmap(source: Bitmap): Bitmap {
    val size = min(source.width, source.height)
    if (size <= 0) return source

    val x = (source.width - size) / 2
    val y = (source.height - size) / 2
    val square = Bitmap.createBitmap(source, x, y, size, size)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    val radius = size / 2f
    canvas.drawCircle(radius, radius, radius, paint)
    return output
}
