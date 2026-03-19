import { ResetButton } from './ResetButton'

export function Sidebar() {
  const navItems = [
    { label: 'Request Log', active: true },
    { label: 'Org State', active: false },
    { label: 'Schema', active: false },
    { label: 'Settings', active: false },
  ]

  return (
    <div className="w-64 bg-gray-900 border-r border-gray-800 flex flex-col h-full">
      <div className="p-4 border-b border-gray-800">
        <h1 className="text-white font-bold text-lg">SF LocalStack</h1>
        <p className="text-gray-400 text-xs mt-1">Salesforce API Emulator</p>
      </div>

      <nav className="flex-1 p-3 space-y-1">
        {navItems.map((item) => (
          <div
            key={item.label}
            className={`px-3 py-2 rounded text-sm
              ${item.active
                ? 'bg-blue-600 text-white cursor-pointer'
                : 'text-gray-500 cursor-not-allowed'}`}
          >
            {item.label}
          </div>
        ))}
      </nav>

      <div className="p-3 border-t border-gray-800">
        <ResetButton />
      </div>
    </div>
  )
}
