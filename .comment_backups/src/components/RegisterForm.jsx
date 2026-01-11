import FormField from './FormField.jsx'
import PasswordField from './PasswordField.jsx'

function RegisterForm({ formValues, formErrors, onFormChange, onSubmit, passwordVisibility, onTogglePassword, motionSafe, accent, isSubmitting }) {
  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <FormField
        id="username"
        label="Username"
        value={formValues.username}
        autoComplete="username"
        onChange={onFormChange}
      />
      {formErrors.username && <p className="text-xs text-rose-300/90">{formErrors.username}</p>}
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
        autoComplete="new-password"
        onChange={onFormChange}
        visible={passwordVisibility.register}
        onToggle={() => onTogglePassword('register')}
        ariaLabel={passwordVisibility.register ? 'Hide password' : 'Show password'}
      />
      {formErrors.password && <p className="text-xs text-rose-300/90">{formErrors.password}</p>}
      <PasswordField
        id="confirmPassword"
        label="Confirm Password"
        value={formValues.confirmPassword}
        autoComplete="new-password"
        onChange={onFormChange}
        visible={passwordVisibility.confirm}
        onToggle={() => onTogglePassword('confirm')}
        ariaLabel={passwordVisibility.confirm ? 'Hide confirm password' : 'Show confirm password'}
        required
      />
      {formErrors.confirmPassword && <p className="text-xs text-rose-300/90">{formErrors.confirmPassword}</p>}
      <button
        type="submit"
        disabled={isSubmitting}
        className="w-full rounded-2xl border border-white/10 px-5 py-3 text-base font-semibold text-white shadow-lg focus:outline-none focus:ring-2 disabled:opacity-60"
        style={{
          backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`,
          boxShadow: `0 10px 30px ${accent.glow}`,
        }}
      >
        {isSubmitting ? 'Registering...' : 'Create account'}
      </button>
      <p className="text-xs text-slate-300/80">
        Secure your profile to start rating, reviewing, and joining anime circles.
      </p>
    </form>
  )
}

export default RegisterForm
