import { useMemo } from 'react'
import QuoteLanding from './QuoteLanding'
import QuoteResults from './QuoteResults'
import BorrowerAuth from './BorrowerAuth'
import QuoteRefine from './QuoteRefine'
import QuoteMatches from './QuoteMatches'
import CalculatorsView from './CalculatorsView'
import BorrowerShell from './BorrowerShell'
import MyQuotes from './MyQuotes'

const screenMap = {
  home: 'home',
  results: 'quote-results',
  auth: 'auth',
  refine: 'quote-refine',
  matches: 'quote-matches',
  tools: 'tools',
  quotes: 'my-quotes',
}

export default function BorrowerApp({
  activeView,
  setActiveView,
  publicQuoteForm,
  setPublicQuoteForm,
  handlePublicQuoteSubmit,
  locationOptions,
  quoteResult,
  loadingTarget,
  handleInput,
  loginForm,
  setLoginForm,
  registerForm,
  setRegisterForm,
  handleLoginSubmit,
  handleRegisterSubmit,
  authState,
  onSignOut,
  refineForm,
  setRefineForm,
  handleRefineQuoteSubmit,
  handleRefineProgressSave,
  matchedLenders,
  matchedAgents,
  errorMessage,
  quoteHistory,
  onSelectBorrowerQuote,
  onDeleteBorrowerQuote,
  activeCalculator,
  setActiveCalculator,
  mortgageForm,
  setMortgageForm,
  mortgageResult,
  amortizationForm,
  setAmortizationForm,
  amortizationResult,
  handleMortgageSubmit,
  handleAmortizationSubmit,
}) {
  const resolvedView = useMemo(() => {
    if (typeof window === 'undefined') {
      return activeView
    }
    const screen = new URLSearchParams(window.location.search).get('screen')
    return screen && screenMap[screen] ? screenMap[screen] : activeView
  }, [activeView])

  const commonProps = {
    setActiveView,
    authState,
    onSignOut,
    activeView: resolvedView,
  }

  if (resolvedView === 'my-quotes') {
    return (
      <MyQuotes
        {...commonProps}
        quoteHistory={quoteHistory}
        onSelectQuote={onSelectBorrowerQuote}
        onDeleteQuote={onDeleteBorrowerQuote}
      />
    )
  }
  if (resolvedView === 'quote-results') {
    return (
      <QuoteResults
        {...commonProps}
        quoteResult={quoteResult}
      />
    )
  }
  if (resolvedView === 'auth') {
    return (
      <BorrowerAuth
        {...commonProps}
        loginForm={loginForm}
        setLoginForm={setLoginForm}
        registerForm={registerForm}
        setRegisterForm={setRegisterForm}
        handleLoginSubmit={handleLoginSubmit}
        handleRegisterSubmit={handleRegisterSubmit}
        loadingTarget={loadingTarget}
        handleInput={handleInput}
        errorMessage={errorMessage}
      />
    )
  }
  if (resolvedView === 'quote-refine') {
    if (!authState?.accessToken) {
      return (
        <BorrowerAuth
          {...commonProps}
          loginForm={loginForm}
          setLoginForm={setLoginForm}
          registerForm={registerForm}
          setRegisterForm={setRegisterForm}
          handleLoginSubmit={handleLoginSubmit}
          handleRegisterSubmit={handleRegisterSubmit}
          loadingTarget={loadingTarget}
          handleInput={handleInput}
          errorMessage={errorMessage}
        />
      )
    }
    return (
      <QuoteRefine
        {...commonProps}
        refineForm={refineForm}
        setRefineForm={setRefineForm}
        handleRefineQuoteSubmit={handleRefineQuoteSubmit}
        handleRefineProgressSave={handleRefineProgressSave}
        quoteResult={quoteResult}
        locationOptions={locationOptions}
        loadingTarget={loadingTarget}
        handleInput={handleInput}
        errorMessage={errorMessage}
      />
    )
  }
  if (resolvedView === 'tools') {
    return (
      <BorrowerShell setActiveView={setActiveView} authState={authState} onSignOut={onSignOut} activeView="tools">
        <CalculatorsView
          activeCalculator={activeCalculator}
          setActiveCalculator={setActiveCalculator}
          mortgageForm={mortgageForm}
          setMortgageForm={setMortgageForm}
          mortgageResult={mortgageResult}
          amortizationForm={amortizationForm}
          setAmortizationForm={setAmortizationForm}
          amortizationResult={amortizationResult}
          loadingTarget={loadingTarget}
          handleMortgageSubmit={handleMortgageSubmit}
          handleAmortizationSubmit={handleAmortizationSubmit}
          handleInput={handleInput}
        />
      </BorrowerShell>
    )
  }
  if (resolvedView === 'quote-matches') {
    return (
      <QuoteMatches
        {...commonProps}
        matchedLenders={matchedLenders}
        matchedAgents={matchedAgents}
        quoteResult={quoteResult}
      />
    )
  }
  return (
    <QuoteLanding
      {...commonProps}
      publicQuoteForm={publicQuoteForm}
      setPublicQuoteForm={setPublicQuoteForm}
      handlePublicQuoteSubmit={handlePublicQuoteSubmit}
      locationOptions={locationOptions}
      quoteResult={quoteResult}
      loadingTarget={loadingTarget}
      handleInput={handleInput}
    />
  )
}
