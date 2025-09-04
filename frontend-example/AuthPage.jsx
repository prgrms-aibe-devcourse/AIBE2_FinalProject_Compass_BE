import React, { useState } from 'react';
import './AuthPage.css';

const AuthPage = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    nickname: '',
    passwordConfirm: '',
    agreeTerms: false
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
    
    // Clear error for this field
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email) {
      newErrors.email = '이메일을 입력해주세요';
    } else if (!emailRegex.test(formData.email)) {
      newErrors.email = '올바른 이메일 형식이 아닙니다';
    }

    // Password validation
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!formData.password) {
      newErrors.password = '비밀번호를 입력해주세요';
    } else if (!isLogin && !passwordRegex.test(formData.password)) {
      newErrors.password = '비밀번호는 8자 이상, 영문 대/소문자, 숫자, 특수문자를 포함해야 합니다';
    }

    // Signup specific validations
    if (!isLogin) {
      if (!formData.nickname) {
        newErrors.nickname = '닉네임을 입력해주세요';
      } else if (formData.nickname.length < 2 || formData.nickname.length > 20) {
        newErrors.nickname = '닉네임은 2-20자 사이여야 합니다';
      }

      if (!formData.passwordConfirm) {
        newErrors.passwordConfirm = '비밀번호 확인을 입력해주세요';
      } else if (formData.password !== formData.passwordConfirm) {
        newErrors.passwordConfirm = '비밀번호가 일치하지 않습니다';
      }

      if (!formData.agreeTerms) {
        newErrors.agreeTerms = '이용약관에 동의해주세요';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      const endpoint = isLogin ? '/api/auth/login' : '/api/auth/signup';
      const payload = isLogin 
        ? { email: formData.email, password: formData.password }
        : { 
            email: formData.email, 
            password: formData.password, 
            nickname: formData.nickname 
          };

      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload)
      });

      const data = await response.json();

      if (response.ok) {
        if (isLogin) {
          // Store tokens
          localStorage.setItem('accessToken', data.accessToken);
          localStorage.setItem('refreshToken', data.refreshToken);
          
          // Show success message
          showNotification('로그인 성공! 여행을 시작해볼까요? ✈️', 'success');
          
          // Redirect to main page
          setTimeout(() => {
            window.location.href = '/main';
          }, 1500);
        } else {
          showNotification('회원가입이 완료되었습니다! 🎉', 'success');
          
          // Switch to login tab
          setIsLogin(true);
          setFormData({
            email: formData.email,
            password: '',
            nickname: '',
            passwordConfirm: '',
            agreeTerms: false
          });
        }
      } else {
        // Handle errors
        if (response.status === 409) {
          setErrors({ email: '이미 사용 중인 이메일입니다' });
        } else if (response.status === 401) {
          setErrors({ password: '이메일 또는 비밀번호가 올바르지 않습니다' });
        } else {
          showNotification(data.message || '요청 처리 중 오류가 발생했습니다', 'error');
        }
      }
    } catch (error) {
      console.error('Error:', error);
      showNotification('서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSocialLogin = async (provider) => {
    showNotification(`${provider} 로그인 준비 중...`, 'info');
    window.location.href = `${API_BASE_URL}/oauth2/authorization/${provider.toLowerCase()}`;
  };

  const showNotification = (message, type) => {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    document.body.appendChild(notification);

    // Remove after 3 seconds
    setTimeout(() => {
      notification.remove();
    }, 3000);
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <div className="logo-section">
            <div className="logo-icon">✈️</div>
            <h1 className="logo-text">Travel Agent</h1>
          </div>
          <h2 className="welcome-title">
            안녕하세요! 어디로 떠나볼까요? <span className="emoji">🟠</span>
          </h2>
          <p className="welcome-subtitle">
            AI 여행 어시스턴트가 완벽한 여행을 계획해드릴게요
          </p>
        </div>

        <div className="auth-body">
          <div className="tab-buttons">
            <button 
              className={`tab-button ${isLogin ? 'active' : ''}`}
              onClick={() => setIsLogin(true)}
            >
              로그인
            </button>
            <button 
              className={`tab-button ${!isLogin ? 'active' : ''}`}
              onClick={() => setIsLogin(false)}
            >
              회원가입
            </button>
          </div>

          <form onSubmit={handleSubmit} className="auth-form">
            <div className="form-group">
              <label htmlFor="email" className="form-label">이메일</label>
              <input
                type="email"
                id="email"
                name="email"
                className={`form-input ${errors.email ? 'error' : ''}`}
                placeholder="example@email.com"
                value={formData.email}
                onChange={handleInputChange}
                autoComplete="email"
              />
              {errors.email && <span className="error-message">{errors.email}</span>}
            </div>

            {!isLogin && (
              <div className="form-group">
                <label htmlFor="nickname" className="form-label">닉네임</label>
                <input
                  type="text"
                  id="nickname"
                  name="nickname"
                  className={`form-input ${errors.nickname ? 'error' : ''}`}
                  placeholder="여행자 닉네임"
                  value={formData.nickname}
                  onChange={handleInputChange}
                />
                {errors.nickname && <span className="error-message">{errors.nickname}</span>}
              </div>
            )}

            <div className="form-group">
              <label htmlFor="password" className="form-label">비밀번호</label>
              <input
                type="password"
                id="password"
                name="password"
                className={`form-input ${errors.password ? 'error' : ''}`}
                placeholder={isLogin ? "비밀번호를 입력하세요" : "8자 이상, 영문/숫자/특수문자 포함"}
                value={formData.password}
                onChange={handleInputChange}
                autoComplete={isLogin ? "current-password" : "new-password"}
              />
              {errors.password && <span className="error-message">{errors.password}</span>}
            </div>

            {!isLogin && (
              <>
                <div className="form-group">
                  <label htmlFor="passwordConfirm" className="form-label">비밀번호 확인</label>
                  <input
                    type="password"
                    id="passwordConfirm"
                    name="passwordConfirm"
                    className={`form-input ${errors.passwordConfirm ? 'error' : ''}`}
                    placeholder="비밀번호를 다시 입력하세요"
                    value={formData.passwordConfirm}
                    onChange={handleInputChange}
                    autoComplete="new-password"
                  />
                  {errors.passwordConfirm && <span className="error-message">{errors.passwordConfirm}</span>}
                </div>

                <div className="form-group checkbox-group">
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      name="agreeTerms"
                      checked={formData.agreeTerms}
                      onChange={handleInputChange}
                    />
                    <span className="checkbox-text">
                      <a href="/terms" target="_blank">이용약관</a> 및{' '}
                      <a href="/privacy" target="_blank">개인정보처리방침</a>에 동의합니다
                    </span>
                  </label>
                  {errors.agreeTerms && <span className="error-message">{errors.agreeTerms}</span>}
                </div>
              </>
            )}

            {isLogin && (
              <div className="forgot-password">
                <a href="/forgot-password">비밀번호를 잊으셨나요?</a>
              </div>
            )}

            <button 
              type="submit" 
              className={`submit-button ${loading ? 'loading' : ''}`}
              disabled={loading}
            >
              {loading ? (
                <span className="loading-spinner">처리 중...</span>
              ) : (
                isLogin ? '로그인' : '회원가입'
              )}
            </button>
          </form>

          <div className="divider">
            <span className="divider-line"></span>
            <span className="divider-text">또는</span>
            <span className="divider-line"></span>
          </div>

          <div className="social-buttons">
            <button 
              className="social-button google"
              onClick={() => handleSocialLogin('Google')}
            >
              <img src="/images/google-icon.svg" alt="Google" className="social-icon" />
              <span>Google 계정으로 시작</span>
            </button>
            <button 
              className="social-button kakao"
              onClick={() => handleSocialLogin('Kakao')}
            >
              <img src="/images/kakao-icon.svg" alt="Kakao" className="social-icon" />
              <span>카카오 계정으로 시작</span>
            </button>
          </div>
        </div>
      </div>

      <div className="travel-cards">
        <div className="travel-card">
          <div className="card-icon">🏝️</div>
          <h3>제주도 여행</h3>
          <p>한국의 하와이에서 힐링</p>
        </div>
        <div className="travel-card">
          <div className="card-icon">🍜</div>
          <h3>부산 맛집</h3>
          <p>돼지국밥부터 밀면까지</p>
        </div>
        <div className="travel-card">
          <div className="card-icon">🏛️</div>
          <h3>경주 역사</h3>
          <p>천년 고도의 문화유산</p>
        </div>
        <div className="travel-card">
          <div className="card-icon">🚗</div>
          <h3>당일치기</h3>
          <p>가까운 곳에서 즐기기</p>
        </div>
      </div>
    </div>
  );
};

export default AuthPage;