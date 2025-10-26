package com.example.answer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * 간결한 로그인 화면 - ChatGPT 스타일
 * 빠른 로그인으로 저장 기능 활성화
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    /* UI Components */
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button signInButton;
    private Button googleSignInButton;
    private TextView skipButton;
    private TextView signUpText;

    /* Firebase */
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase Auth 초기화
        mAuth = FirebaseAuth.getInstance();

        // Google Sign-In 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.project_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initViews();
        setupClickListeners();

        Log.d(TAG, "LoginActivity initialized");
    }

    private void initViews() {
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        signInButton = findViewById(R.id.sign_in_button);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        skipButton = findViewById(R.id.skip_button);
        signUpText = findViewById(R.id.sign_up_text);
    }

    private void setupClickListeners() {
        // 이메일 로그인
        signInButton.setOnClickListener(this::signInWithEmail);

        // Google 로그인
        googleSignInButton.setOnClickListener(this::signInWithGoogle);

        // 건너뛰기 (게스트 모드)
        skipButton.setOnClickListener(this::skipLogin);

        // 회원가입
        signUpText.setOnClickListener(this::showSignUp);
    }

    /**
     * 이메일 로그인
     */
    private void signInWithEmail(View view) {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        proceedToMain(user);
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Google 로그인
     */
    private void signInWithGoogle(View view) {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * 로그인 건너뛰기 (게스트 모드)
     */
    private void skipLogin(View view) {
        Log.d(TAG, "User chose to continue without login (guest mode)");
        Toast.makeText(this, "Guest mode: Recording works, but history requires login",
                Toast.LENGTH_LONG).show();
        proceedToMain(null); // null = 게스트 모드
    }

    /**
     * 회원가입 다이얼로그 (간단한 구현)
     */
    private void showSignUp(View view) {
        // 간단한 회원가입 - 이메일과 비밀번호만
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View signUpView = getLayoutInflater().inflate(R.layout.diagnal_sign_up, null);

        EditText emailField = signUpView.findViewById(R.id.signup_email);
        EditText passwordField = signUpView.findViewById(R.id.signup_password);
        EditText confirmPasswordField = signUpView.findViewById(R.id.signup_confirm_password);

        builder.setView(signUpView)
                .setTitle("Sign Up")
                .setPositiveButton("Create Account", (dialog, which) -> {
                    String email = emailField.getText().toString().trim();
                    String password = passwordField.getText().toString().trim();
                    String confirmPassword = confirmPasswordField.getText().toString().trim();

                    if (email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!password.equals(confirmPassword)) {
                        Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (password.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createAccount(email, password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * 계정 생성
     */
    private void createAccount(String email, String password) {
        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        proceedToMain(user);
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Account creation failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        proceedToMain(user);
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 메인 화면으로 이동
     */
    private void proceedToMain(FirebaseUser user) {
        Intent intent = new Intent(this, MainActivity.class);
        if (user != null) {
            intent.putExtra("user_logged_in", true);
            intent.putExtra("user_email", user.getEmail());
            intent.putExtra("user_name", user.getDisplayName());
            Log.d(TAG, "Proceeding to MainActivity as logged-in user: " + user.getEmail());
        } else {
            intent.putExtra("user_logged_in", false);
            Log.d(TAG, "Proceeding to MainActivity as guest user");
        }
        startActivity(intent);
        finish();
    }

    /**
     * 로딩 상태 UI 업데이트
     */
    private void setLoading(boolean isLoading) {
        signInButton.setEnabled(!isLoading);
        googleSignInButton.setEnabled(!isLoading);
        skipButton.setEnabled(!isLoading);

        if (isLoading) {
            signInButton.setText("Signing in...");
        } else {
            signInButton.setText("Sign In");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 자동 로그인 체크
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Auto-login: User already signed in");
            proceedToMain(currentUser);
        }
    }
}