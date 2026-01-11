import FormField from './FormField.jsx'
import PasswordField from './PasswordField.jsx'

function LoginForm({ formValues, formErrors, onFormChange, onSubmit, passwordVisibility, onTogglePassword, motionSafe, accent, isSubmitting }) {
  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <FormField
        id="email"
        label="Email"
        type="email"
        value={formValues.email}
        autoComplete="email"
        onChange={onFormChange}
      />
      {formErrors.email && <p className="text-xs text-rose-300/90">{formErrors.email}</p>}
      <PasswordField
        id="password"
        label="Password"
        value={formValues.password}
        autoComplete="current-password"
        onChange={onFormChange}
        visible={passwordVisibility.login}
        onToggle={() => onTogglePassword('login')}
        ariaLabel={passwordVisibility.login ? 'Hide password' : 'Show password'}
      />
      {formErrors.password && <p className="text-xs text-rose-300/90">{formErrors.password}</p>}
      <div className="flex items-center justify-between text-sm text-slate-200/80">
        <label className="inline-flex items-center gap-2">
          <input
            type="checkbox"
            className="h-4 w-4 rounded border-white/20 bg-white/10 focus:ring-2"
            style={{ accentColor: accent.strong }}
          />
          <span>Remember me</span>
        </label>
        <a href="/forgot-password" className="font-semibold text-purple-200 hover:text-white">Forgot?</a>
      </div>
      <button
        type="submit"
        className="w-full rounded-2xl border border-white/10 px-5 py-3 text-base font-semibold text-white shadow-lg focus:outline-none focus:ring-2"
        style={{
          backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`,
          boxShadow: `0 10px 30px ${accent.glow}`,
        }}
        disabled={isSubmitting}
      >
        {isSubmitting ? 'Signing in...' : 'Continue to feed'}
      </button>
      <p className="text-xs text-slate-300/80">
        By continuing, you agree to the community rules about respectful debate and spoiler tags.
      </p>
    </form>
  )
}

export default LoginForm
