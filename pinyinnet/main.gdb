define dbg_preProcess
  b 131
  commands
    p i
    p j
    continue
  end
end

define dbg_preProcess2
  b 141
  commands
    p arcArray[i]
    echo sizeof(int) * cHit
  end
end

define dbg_preProcess3
  break 145
  commands
    echo "cHit:"
    p cHit, sizeof
  end
end