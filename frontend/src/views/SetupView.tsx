import { useState } from 'react'
import { Settings, Trash2 } from 'lucide-react'
import { resetOrg, listUsers, deleteUser as apiDeleteUser } from '../services/api'
import { PageHeader } from '../components/ui/PageHeader'
import { Modal } from '../components/ui/Modal'
import { useToast } from '../components/ui/Toast'

interface UserRecord {
  id: string
  username: string
  email?: string
  role?: string
}

interface Props {
  currentUser: { username: string; role: string }
  apiVersion: string
}

export function SetupView({ currentUser, apiVersion }: Props) {
  const [users, setUsers] = useState<UserRecord[]>([])
  const [usersLoaded, setUsersLoaded] = useState(false)
  const [showResetConfirm, setShowResetConfirm] = useState(false)
  const [resetting, setResetting] = useState(false)
  const { showSuccess, showError } = useToast()

  async function loadUsers() {
    try {
      const result = await listUsers()
      setUsers(result)
      setUsersLoaded(true)
    } catch {
      // Auth endpoint may not exist; silently ignore
      setUsersLoaded(true)
    }
  }

  async function handleDeleteUser(id: string) {
    try {
      await apiDeleteUser(id)
      showSuccess('User deleted')
      setUsers((prev) => prev.filter((u) => u.id !== id))
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to delete user')
    }
  }

  async function handleReset() {
    setResetting(true)
    try {
      await resetOrg()
      showSuccess('Org reset successfully')
      setShowResetConfirm(false)
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to reset org')
    } finally {
      setResetting(false)
    }
  }

  if (!usersLoaded) {
    void loadUsers()
  }

  return (
    <div className="flex h-full flex-col">
      <PageHeader
        title="Setup"
        subtitle="Administration and configuration"
        icon={<Settings className="h-5 w-5" />}
      />

      <div className="flex-1 overflow-auto p-6">
        <div className="mx-auto max-w-4xl space-y-6">
          {/* Org Settings */}
          <div className="rounded-slds border border-neutral-20 bg-neutral-00">
            <div className="border-b border-neutral-20 px-6 py-4">
              <h2 className="text-heading-sm font-bold text-neutral-90">Org Settings</h2>
            </div>
            <div className="p-6">
              <div className="grid gap-6 sm:grid-cols-2">
                <div>
                  <div className="text-body-sm text-neutral-50">API Version</div>
                  <div className="mt-0.5 text-body-md font-medium text-neutral-80">{apiVersion || 'v60.0'}</div>
                </div>
                <div>
                  <div className="text-body-sm text-neutral-50">Current User</div>
                  <div className="mt-0.5 text-body-md font-medium text-neutral-80">{currentUser.username} ({currentUser.role})</div>
                </div>
              </div>

              <div className="mt-6">
                <button
                  type="button"
                  onClick={() => setShowResetConfirm(true)}
                  className="rounded-slds border border-destructive/20 bg-neutral-00 px-4 py-2 text-body-md font-medium text-destructive hover:bg-error/5"
                >
                  Reset Org
                </button>
                <p className="mt-2 text-body-sm text-neutral-50">
                  This will wipe all data and restore the seed baseline. This action cannot be undone.
                </p>
              </div>
            </div>
          </div>

          {/* User Management */}
          <div className="rounded-slds border border-neutral-20 bg-neutral-00">
            <div className="border-b border-neutral-20 px-6 py-4">
              <h2 className="text-heading-sm font-bold text-neutral-90">User Management</h2>
            </div>
            <div className="p-6">
              {users.length === 0 ? (
                <p className="text-body-md text-neutral-50">No users loaded or auth system unavailable.</p>
              ) : (
                <table className="w-full text-body-md">
                  <thead>
                    <tr className="border-b border-neutral-20 bg-neutral-05">
                      <th className="px-4 py-2 text-left text-body-sm font-semibold uppercase text-neutral-70">Username</th>
                      <th className="px-4 py-2 text-left text-body-sm font-semibold uppercase text-neutral-70">Email</th>
                      <th className="px-4 py-2 text-left text-body-sm font-semibold uppercase text-neutral-70">Role</th>
                      <th className="px-4 py-2 text-right text-body-sm font-semibold uppercase text-neutral-70">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((user) => (
                      <tr key={user.id} className="border-b border-neutral-10">
                        <td className="px-4 py-2.5 text-neutral-80">{user.username}</td>
                        <td className="px-4 py-2.5 text-neutral-60">{user.email ?? '--'}</td>
                        <td className="px-4 py-2.5 text-neutral-60">{user.role ?? 'user'}</td>
                        <td className="px-4 py-2.5 text-right">
                          {currentUser.role === 'admin' && user.username !== currentUser.username && (
                            <button
                              type="button"
                              onClick={() => { void handleDeleteUser(user.id) }}
                              className="inline-flex items-center gap-1 rounded-slds p-1 text-error hover:bg-error/5"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      </div>

      <Modal
        isOpen={showResetConfirm}
        onClose={() => setShowResetConfirm(false)}
        title="Confirm Reset"
        actions={
          <>
            <button type="button" onClick={() => setShowResetConfirm(false)} className="rounded-slds border border-neutral-20 bg-neutral-00 px-4 py-2 text-body-md font-medium text-neutral-70 hover:bg-neutral-05">Cancel</button>
            <button type="button" onClick={() => { void handleReset() }} disabled={resetting} className="rounded-slds bg-destructive px-4 py-2 text-body-md font-semibold text-neutral-00 hover:bg-error disabled:opacity-60">{resetting ? 'Resetting...' : 'Reset Org'}</button>
          </>
        }
      >
        <p className="text-body-md text-neutral-70">
          This will wipe all data and restore the seed baseline. All records, metadata, and bulk jobs will be deleted. Are you sure?
        </p>
      </Modal>
    </div>
  )
}
