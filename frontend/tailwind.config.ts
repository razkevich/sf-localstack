import type { Config } from 'tailwindcss'

const config: Config = {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: { DEFAULT: '#0176d3', dark: '#014486', light: '#1b96ff', lighter: '#d8edff' },
        success: { DEFAULT: '#2e844a', light: '#45c65a' },
        warning: { DEFAULT: '#fe9339', light: '#ffb75d' },
        error: { DEFAULT: '#ea001e', light: '#fe5c4c' },
        destructive: '#ba0517',
        neutral: {
          '00': '#ffffff',
          '05': '#f3f3f3',
          '10': '#ecebea',
          '20': '#dddbda',
          '30': '#c9c7c5',
          '40': '#b0adab',
          '50': '#969492',
          '60': '#706e6b',
          '70': '#514f4d',
          '80': '#3e3e3c',
          '90': '#201b1b',
        },
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'Helvetica', 'Arial', 'sans-serif'],
      },
      fontSize: {
        'heading-lg': ['1.5rem', { lineHeight: '1.25' }],
        'heading-md': ['1.25rem', { lineHeight: '1.25' }],
        'heading-sm': ['1rem', { lineHeight: '1.25' }],
        'body-md': ['0.875rem', { lineHeight: '1.5' }],
        'body-sm': ['0.75rem', { lineHeight: '1.5' }],
      },
      borderRadius: { slds: '0.25rem' },
      boxShadow: {
        slds: '0 2px 4px rgba(0,0,0,0.1)',
        'slds-lg': '0 4px 8px rgba(0,0,0,0.1)',
      },
    },
  },
  plugins: [],
}

export default config
